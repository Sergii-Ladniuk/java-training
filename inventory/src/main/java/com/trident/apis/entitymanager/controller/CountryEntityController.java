package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.apis.entitymanager.model.Country;
import com.trident.shared.immigration.repository.criteria.*;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.*;
import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/countries", description = "CRUD operations with countries")
public class CountryEntityController {

    @Autowired
    private RepositoryDecorator<Country> repository;

    @ApiOperation(value = "Load country by ID", response = Country.class, httpMethod = "GET")
    @RequestMapping("countries/{countryId}")
    public Country countryById(@PathVariable String countryId) {
        return repository.findOne(countryId);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load countries list", response = Country[].class, httpMethod = "GET", responseContainer = "List")
    @RequestMapping("countries")
    public Iterable<Country> allCountries(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME)
            @RequestParam(required = false) String name,
            @ApiParam(value = FILTER_BY_CODE, name = CODE)
            @RequestParam(required = false) String code) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, code);
        return repository.findAll(sort, searchQuery);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load countries page", httpMethod = "GET")
    @RequestMapping("countries/page")
    public Page<Country> allCountries(
            Pageable pageable,
            @ApiParam(FILTER_BY_NAME)
            @RequestParam(required = false) String name,
            @ApiParam(FILTER_BY_CODE)
            @RequestParam(required = false) String code) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, code);
        return repository.findAll(pageable, searchQuery);
    }

    private SearchCriteriaList getSearchCriterias(String name, String code) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name)),
                new SearchLikeCriteria(CODE, code));
    }

    @ApiOperation(value = "Save a new country", response = Country.class, httpMethod = "POST")
    @RequestMapping(value = "countries", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public Country insertCountry(@RequestBody Country country, HttpServletRequest request) {
        makeSureIdPresent(country);
        return repository.insert(country, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save multiple countries", response = Country[].class, httpMethod = "POST")
    @RequestMapping(value = "countries/list", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public Iterable<Country> insertCountries(@RequestBody List<Country> countries, HttpServletRequest request) {
        countries.forEach(this::makeSureIdPresent);
        return repository.bulkSaveWithoutHistory(countries, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update a country", response = Country.class, httpMethod = "PUT")
    @RequestMapping(value = {"countries", "countries/{countryId}"}, method = RequestMethod.PUT, consumes = APPLICATION_JSON_UTF8_VALUE)
    public Country updateCountry(@RequestBody Country country, HttpServletRequest request) {
        makeSureIdPresent(country);
        return repository.update(country, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete a country", response = Country.class, httpMethod = "DELETE")
    @RequestMapping(value = "countries/{countryId}", method = RequestMethod.DELETE)
    public SimpleResponse deleteCountry(@PathVariable("countryId") String countryId, HttpServletRequest request) {
        repository.delete(countryId, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all countries", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "countries", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        repository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by CountryDto id", httpMethod = "GET")
    @GetMapping(value = "countries/history/{id}")
    public List<HistoryEntity<?>> getHistoryById(@PathVariable("id") String id) {
        return repository.getHistoryById(id);
    }

    @ApiOperation(value = "Loads history by provided filters", httpMethod = "GET")
    @GetMapping(value = "countries/history")
    public Page<HistoryEntity<?>> getHistory(
            Pageable pageable,
            @ApiParam(required = false, name = USER_ID, value = FILTER_BY_USER_ID) @RequestParam(required = false) String userId,
            @ApiParam(required = false, name = HISTORY_TYPE, value = FILTER_BY_HISTORY_TYPE) @RequestParam(required = false) String historyType,
            @ApiParam(required = false, name = PARAM_DATE_FROM, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateFrom,
            @ApiParam(required = false, name = PARAM_DATE_TO, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateTo) {
        SearchCriteriaList criterias = new SearchCriteriaList();
        if (!StringUtils.isEmpty(userId)) {
            criterias.add(new SearchLikeCriteria(USER_ID, userId));
        }
        if (!StringUtils.isEmpty(historyType)) {
            criterias.add(new SearchEqualCriteria<>(HISTORY_TYPE, historyType));
        }
        Long fixedDateTo = dateTo;
        if (dateFrom != null && fixedDateTo == null) {
            fixedDateTo = System.currentTimeMillis();
        }
        if (dateFrom != null) {
            criterias.add(new SearchBetweenCriteria("updateDate", dateFrom, fixedDateTo));
        }
        return repository.findAllHistory(pageable, criterias);
    }

    private void makeSureIdPresent(Country country) {
        if (country.getId() == null) {
            country.setId(UUID.randomUUID().toString());
        }
    }
}
