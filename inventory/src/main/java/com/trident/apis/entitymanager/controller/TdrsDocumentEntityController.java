package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchIdInCriteria;
import com.trident.shared.immigration.repository.criteria.SearchLikeCriteria;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static com.trident.shared.immigration.constants.ControllerConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping(path = "/api/v1/tdrs/documents", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/tdrs/documents", description = "CRUD operations with TDRS documents")
public class TdrsDocumentEntityController {

    private static Logger logger = Logger.getLogger(TdrsDocumentEntityController.class);

    @Autowired
    private RepositoryDecorator<TdrsDocument> repository;

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load Tdrs Documents list", httpMethod = "GET")
    @GetMapping("")
    public Iterable<TdrsDocument> getAll(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME)
            @RequestParam(required = false) String name) {
        SearchCriteriaList searchQuery = getSearchCriterias(name);
        return repository.findAll(sort, searchQuery);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load Tdrs Documents page", httpMethod = "GET")
    @GetMapping("page")
    public Page<TdrsDocument> getPage(
            Pageable pageable,
            @ApiParam(FILTER_BY_NAME)
            @RequestParam(required = false) String name) {
        SearchCriteriaList searchQuery = getSearchCriterias(name);
        return repository.findAll(pageable, searchQuery);
    }

    private SearchCriteriaList getSearchCriterias(String name) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name)));
    }

    @ApiOperation(value = "Load a Tdrs Document by ID", response = TdrsDocument.class, httpMethod = "GET")
    @GetMapping("{id}")
    public TdrsDocument getById(@PathVariable("id") String id) {
        return repository.findOne(id);
    }

    @ApiOperation(value = "Load a Tdrs Document list by Tdrs Document IDs", response = TdrsDocument[].class, httpMethod = "GET")
    @RequestMapping(value = "listByIds", method = RequestMethod.GET)
    public Iterable<TdrsDocument> tdrsDocumentsById(@RequestParam("id") List<String> ids) {
        return repository.findAll(new SearchCriteriaList(new SearchIdInCriteria(ids)));
    }

    @ApiOperation(value = "Save a new Tdrs Document", response = TdrsDocument.class, httpMethod = "POST")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public TdrsDocument insert(@RequestBody @Valid TdrsDocument tdrsDocument, HttpServletRequest request) {
        makeSureIdPresent(tdrsDocument);
        return repository.insert(tdrsDocument, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save multiple Tdrs Documents", response = TdrsDocument[].class, httpMethod = "POST")
    @RequestMapping(value = "list", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public Iterable<TdrsDocument> insert(@RequestBody List<TdrsDocument> entities, HttpServletRequest request) {
        logger.debug("Save multiple Tdrs Documents started.");
        try {
            entities.forEach(tdrsDocument -> {
                makeSureIdPresent(tdrsDocument);
            });
            return repository.bulkSaveWithoutHistory(entities, RestUtils.getUserId(request));
        } finally {
            logger.debug("Save multiple Tdrs Documents finished.");
        }
    }

    @ApiOperation(value = "Update a Tdrs Document", response = TdrsDocument.class, httpMethod = "PUT")
    @RequestMapping(value = {"", "{id}"}, method = RequestMethod.PUT)
    public TdrsDocument update(@RequestBody @Valid TdrsDocument tdrsDocument, HttpServletRequest request) {
        makeSureIdPresent(tdrsDocument);
        return repository.update(tdrsDocument, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete a Tdrs Document", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable("id") String id, HttpServletRequest request) {
        repository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all Tdrs Documents", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        repository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by Tdrs Document id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repository.getHistoryById(id);
    }

    private void makeSureIdPresent(TdrsDocument tdrsDocument) {
        if (tdrsDocument.getId() == null) {
            tdrsDocument.setId(UUID.randomUUID().toString());
        }
    }
}
