package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.apis.entitymanager.model.DocumentContent;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchIdInCriteria;
import com.trident.shared.immigration.repository.criteria.SearchLikeCriteria;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static com.trident.shared.immigration.constants.ControllerConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping(path = "/api/v1/apis", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/apis/templates", description = "CRUD operations with APIS Templates")
public class ApisTemplateEntityController {

    @Autowired
    private RepositoryDecorator<ApisTemplate> apisTemplateRepository;
    @Lazy
    @Autowired
    private RepositoryDecorator<DocumentContent> contentRepository;

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, required = false, paramType = QUERY_PARAM_TYPE)})
    @ApiOperation(value = "Load APIS Templates list", response = ApisTemplate[].class, httpMethod = "GET")
    @RequestMapping(value = "templates")
    public Iterable<ApisTemplate> allApisTemplates(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME, required = false) @RequestParam(required = false) String name) {
        SearchCriteriaList searchCriterias = getSearchCriteria(name);
        return apisTemplateRepository.findAll(sort, searchCriterias);
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, required = false, paramType = QUERY_PARAM_TYPE)})
    @ApiOperation(value = "Load APIS Templates page", response = Page.class, httpMethod = "GET")
    @RequestMapping(value = "templates/page")
    public Page<ApisTemplate> allApisTemplatesByPage(
            Pageable pageable,
            @ApiParam(value = FILTER_BY_NAME, name = NAME) @RequestParam(required = false) String name) {
        SearchCriteriaList searchCriterias = getSearchCriteria(name);
        return apisTemplateRepository.findAll(pageable, searchCriterias);
    }

    @ApiOperation(value = "Load APIS Template by ID", response = ApisTemplate.class, httpMethod = "GET")
    @RequestMapping(value = "templates/{documentId}")
    public ApisTemplate apisTemplateById(@PathVariable(name = "documentId") String documentId) {
        return apisTemplateRepository.findOne(documentId);
    }

    @ApiOperation(value = "Load a APIS Template name list by document IDs", response = ApisTemplate[].class, httpMethod = "GET")
    @RequestMapping(value = "templates/namesByIds")
    public Iterable<ApisTemplate> apisTemplateNamesByIds(@RequestParam(name = "id") List<String> ids) {
        return apisTemplateRepository.findAll(ImmutableList.of("name"), new SearchCriteriaList(new SearchIdInCriteria(ids)));
    }

    @ApiOperation(value = "Load a APIS Templates list by document IDs", response = ApisTemplate[].class, httpMethod = "GET")
    @RequestMapping(value = "templates/byIds")
    public Iterable<ApisTemplate> apisTemplatesByIds(@RequestParam(name = "id") List<String> ids) {
        return apisTemplateRepository.findAll(new SearchCriteriaList(new SearchIdInCriteria(ids)));
    }

    @ApiOperation(value = "Save a new APIS Template", response = ApisTemplate.class, httpMethod = "POST")
    @RequestMapping(value = "templates", method = RequestMethod.POST)
    public ApisTemplate insertApisTemplate(@RequestBody @Valid ApisTemplate apisTemplate, HttpServletRequest request) {
        checkIdSet(apisTemplate);
        processContentAndNotes(apisTemplate, RestUtils.getUserId(request));
        return apisTemplateRepository.insert(apisTemplate, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save a new APIS Template", response = ApisTemplate.class, httpMethod = "POST")
    @RequestMapping(value = "templates/list", method = RequestMethod.POST)
    public List<ApisTemplate> insertApisTemplateAll(@RequestBody @Valid List<ApisTemplate> apisTemplates, HttpServletRequest request) {
        checkIdSet(apisTemplates);
        for (ApisTemplate apisTemplate : apisTemplates) {
            processContentAndNotes(apisTemplate, RestUtils.getUserId(request));
        }
        return apisTemplateRepository.bulkSaveWithoutHistory(apisTemplates, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update an existing APIS Template", response = ApisTemplate.class, httpMethod = "PUT")
    @RequestMapping(value = {"templates", "templates/{id}"}, method = RequestMethod.PUT)
    public ApisTemplate updateApisTemplate(@RequestBody @Valid ApisTemplate apisTemplate, HttpServletRequest request) {
        checkIdSet(apisTemplate);
        processContentAndNotes(apisTemplate, RestUtils.getUserId(request));
        return apisTemplateRepository.update(apisTemplate, RestUtils.getUserId(request));
    }

    private void processContentAndNotes(ApisTemplate apisTemplate, String userId) {
        processContent(apisTemplate, userId);
        processNotes(apisTemplate, userId);
    }

    private void processNotes(ApisTemplate apisTemplate, String userId) {
        if (apisTemplate.getNotes() != null) {
            DocumentContent notes;
            if (apisTemplate.getRichFormatNotes() != null && apisTemplate.getRichFormatNotes()) {
                notes = contentRepository.insert(
                        DocumentContent.fromRichFormat(
                                apisTemplate.getNotes(),
                                apisTemplate.getNoteFileName()),
                        userId);
            } else {
                notes = contentRepository.insert(DocumentContent.fromPlainText(
                        apisTemplate.getNotes()),
                        userId);
            }
            apisTemplate.setNoteId(notes.getId());
            apisTemplate.setNotes(null);
        }
    }

    private void processContent(ApisTemplate apisTemplate, String userId) {
        if (StringUtils.isEmpty(apisTemplate.getContentId()) && StringUtils.isEmpty(apisTemplate.getContent())) {
            throw new IllegalArgumentException("ApisTemplateDto should contain content or contentId.");
        }
        if (!StringUtils.isEmpty(apisTemplate.getContent())) {
            DocumentContent content;
            if (apisTemplate.getType().isBase64encoded()) {
                content = contentRepository.insert(
                        DocumentContent.fromRichFormat(
                                apisTemplate.getContent(),
                                apisTemplate.getOriginalFileName()),
                        userId);
            } else {
                content = contentRepository.insert(
                        DocumentContent.fromPlainText(apisTemplate.getContent()),
                        userId);
            }
            apisTemplate.setContentId(content.getId());
            apisTemplate.setContent(null);
        }
    }

    @ApiOperation(value = "Delete APIS Template by ID", response = String.class, httpMethod = "DELETE")
    @RequestMapping(value = "templates/{documentId}", method = RequestMethod.DELETE)
    public SimpleResponse deleteApisTemplate(@PathVariable("documentId") String documentId, HttpServletRequest request) {
        apisTemplateRepository.delete(documentId, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "templates", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        apisTemplateRepository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by ApisTemplateDto id", httpMethod = "GET")
    @GetMapping(value = "templates/history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return apisTemplateRepository.getHistoryById(id);
    }

    private SearchCriteriaList getSearchCriteria(String name) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name))
        );
    }

    private void checkIdSet(List<ApisTemplate> templates) {
        templates.forEach(this::checkIdSet);
    }

    private void checkIdSet(ApisTemplate template) {
        if (template.getId() == null) {
            template.setId(UUID.randomUUID().toString());
        }
    }

}
