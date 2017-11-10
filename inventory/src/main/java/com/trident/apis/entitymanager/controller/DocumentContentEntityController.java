package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.DocumentContent;
import com.trident.shared.immigration.dto.apis.DocumentContentDto;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchIdInCriteria;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static com.trident.shared.immigration.constants.ControllerConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping(path = "/api/v1/apis/document-content", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/apis/document-content", description = "CRUD operations with Document Content")
public class DocumentContentEntityController {

    private static final Logger logger = Logger.getLogger(DocumentContentEntityController.class);

    @Lazy
    @Autowired
    RepositoryDecorator<DocumentContent> documentContentCouchbaseRepository;

    @ApiOperation(value = "Load Document Content by ID", response = DocumentContent.class, httpMethod = "GET")
    @RequestMapping("{id}")
    public DocumentContent documentContentById(@PathVariable String id){ return documentContentCouchbaseRepository.findOne(id);}


    @ApiOperation(value = "Load a Document Content list by document IDs", response = DocumentContentDto[].class, httpMethod = "GET")
    @RequestMapping(value = "byIds")
    public List<DocumentContent> listByIds(@RequestParam(name = "id") List<String> ids) {
        return documentContentCouchbaseRepository.findAll(new SearchCriteriaList(new SearchIdInCriteria(ids)));
    }

    @ApiOperation(value = "Save a new Document Content", response = DocumentContent.class, httpMethod = "POST")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public DocumentContent insertDocumentContent(@RequestBody DocumentContent documentContent, HttpServletRequest request) {
        makeSureIdPresent(ImmutableList.of(documentContent) );
        return documentContentCouchbaseRepository.insert(documentContent, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Bulk save a new Document Content", httpMethod = "POST")
    @RequestMapping(value = "list", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public List<DocumentContent> insertBulkDocumentContent(@RequestBody List<DocumentContent> documentContentList, HttpServletRequest request) {
        makeSureIdPresent(documentContentList);
        return documentContentCouchbaseRepository.bulkSaveWithoutHistory(documentContentList, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete all Document Content", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public SimpleResponse deleteAllDocumentContent(HttpServletRequest request) {
        documentContentCouchbaseRepository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load ports page", httpMethod = "GET")
    @RequestMapping("page")
    public Page<DocumentContent> allPorts(Pageable pageable) {
        return documentContentCouchbaseRepository.findAll(pageable, new SearchCriteriaList());
    }

    private void makeSureIdPresent(List<DocumentContent> documentContentList) {
        documentContentList.forEach(documentContent -> {
            if (documentContent.getId() == null) {
                documentContent.setId(UUID.randomUUID().toString());
            }
        });
    }
}
