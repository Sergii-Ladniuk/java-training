package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.TridentToPortTranslation;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortTranslationDto;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.*;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
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
@RequestMapping(path = "/api/v1/apis/tridentToApis", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/apis/tridentToApis", description = "CRUD operations to config TridentToPortTranslation mapping")
public class TridentToPortTranslationEntityController {

    @Autowired
    private RepositoryDecorator<TridentToPortTranslation> repositoryDecorator;

    @ApiOperation(value = "Loads TridentToPortTranslation list", response = TridentToPortTranslation[].class, httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION),
            @ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, value = FILTER_BY_NAME)
    })
    @GetMapping
    public Iterable<TridentToPortTranslation> list(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME) @RequestParam(required = false) String name) {
        return repositoryDecorator.findAll(sort, new SearchCriteriaList(new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name))));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION),
            @ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, value = FILTER_BY_NAME)
    })
    @GetMapping("page")
    public Page<TridentToPortTranslation> page(
            Pageable pageable,
            @ApiParam(value = FILTER_BY_NAME, name = NAME) @RequestParam(required = false) String name) {
        return repositoryDecorator.findAll(pageable, new SearchCriteriaList(new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name))));
    }


    @ApiOperation(value = "Load TridentToPortTranslation by ID", response = TridentToPortTranslation.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public TridentToPortTranslation getById(@PathVariable(name = "id") String id) {
        return repositoryDecorator.findOne(id);
    }

    @ApiOperation(value = "Load a TridentToPortApisConfig list by IDs", response = TridentToPortTranslationDto[].class, httpMethod = "GET")
    @GetMapping(value = "listByIds")
    public Iterable<TridentToPortTranslation> getByIds(@RequestParam(name = "id") List<String> ids) {
        return repositoryDecorator.findAll(new SearchCriteriaList(new SearchIdInCriteria(ids)));
    }

    @ApiOperation(value = "Save new TridentToPortTranslation", response = TridentToPortTranslation.class, httpMethod = "POST")
    @PostMapping(value = {"", "{id}"})
    public TridentToPortTranslation insert(@RequestBody @Valid TridentToPortTranslation codeDictionary, HttpServletRequest request) {
        checkIfIdSet(codeDictionary);
        return repositoryDecorator.insert(codeDictionary, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save all new TridentToPortTranslation", response = TridentToPortTranslation.class, httpMethod = "POST")
    @PostMapping(value = {"list"})
    public List<TridentToPortTranslation> insertAll(@RequestBody @Valid List<TridentToPortTranslation> codeDictionaryList, HttpServletRequest request) {
        checkIfIdSet(codeDictionaryList);
        return repositoryDecorator.bulkSaveWithoutHistory(codeDictionaryList, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update TridentToPortTranslation", response = TridentToPortTranslation.class, httpMethod = "PUT")
    @PutMapping(value = {"", "{id}"})
    public TridentToPortTranslation update(@RequestBody @Valid TridentToPortTranslation codeDictionary, HttpServletRequest request) {
        checkIfIdSet(codeDictionary);
        return repositoryDecorator.update(codeDictionary, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete TridentToPortTranslation by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable("id") String id, HttpServletRequest request) {
        repositoryDecorator.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by TridentToPortTranslation id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repositoryDecorator.getHistoryById(id);
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        repositoryDecorator.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    private void checkIfIdSet(TridentToPortTranslation codeDictionary) {
        if (codeDictionary.getId() == null) {
            codeDictionary.setId(UUID.randomUUID().toString());
        }
    }

    private void checkIfIdSet(List<TridentToPortTranslation> codeDictionary) {
        codeDictionary.forEach(this::checkIfIdSet);
    }
}
