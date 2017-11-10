package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.apis.entitymanager.service.CountryMapper;
import com.trident.apis.entitymanager.service.tdrs.TdrsRecommendationTreeMapper;
import com.trident.shared.immigration.dto.tdrs.TdrsRuleDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.RecommendationNode;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.*;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.trident.shared.immigration.constants.ControllerConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping(path = "/api/v1/tdrs/rules", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/tdrs/rules", description = "CRUD operations with TDRS Rules")
public class TdrsRuleEntityController {

    private static final Logger logger = Logger.getLogger(TdrsRuleEntityController.class);

    @Autowired
    private RepositoryDecorator<TdrsRule> repository;
    @Autowired
    private TdrsRecommendationTreeMapper tdrsRecommendationTreeMapper;
    @Autowired
    private CountryMapper countryMapper;

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load Tdrs Rule list", httpMethod = "GET", response = TdrsRuleDto[].class)
    @GetMapping("")
    public Iterable<?> getAll(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME)
            @RequestParam(required = false) String name,
            @ApiParam(value = FILTER_BY_COUNTRY, name = DESTINATION_COUNTRY_ID)
            @RequestParam(required = false) String destinationCountryId,
            @ApiParam(value = FILTER_BY_COUNTRY_CODES, name = DESTINATION_COUNTRY_CODES)
            @RequestParam(required = false) List<String> destinationCountryCodes,
            @ApiParam(value = FULL_DESC, name = FULL)
            @RequestParam(required = false, defaultValue = "true") boolean full
    ) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, destinationCountryId, destinationCountryCodes);
        List<TdrsRule> tdrsRules = repository.findAll(sort, searchQuery);
        if (full) {
            return convertListToFullModel(tdrsRules);
        } else {
            return tdrsRules;
        }
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load Tdrs Rule page", httpMethod = "GET")
    @GetMapping("page")
    public Page<TdrsRuleDto> getPage(
            Pageable pageable,
            @ApiParam(FILTER_BY_NAME)
            @RequestParam(required = false) String name,
            @ApiParam(value = FILTER_BY_COUNTRY, name = COUNTRY_ID)
            @RequestParam(required = false) String destinationCountryId,
            @ApiParam(value = FILTER_BY_COUNTRY_CODES, name = DESTINATION_COUNTRY_CODES)
            @RequestParam(required = false) List<String> destinationCountryCodes) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, destinationCountryId, destinationCountryCodes);
        Page<TdrsRule> page = repository.findAll(pageable, searchQuery);
        List<TdrsRule> tdrsRules = page.getContent();
        List<TdrsRuleDto> tdrsRuleDtos = convertListToFullModel(tdrsRules);
        return new PageImpl<TdrsRuleDto>(tdrsRuleDtos, pageable, page.getTotalElements());
    }

    @ApiOperation(value = "Load a Tdrs Rule by ID", response = TdrsRule.class, httpMethod = "GET")
    @GetMapping("{id}")
    public TdrsRuleDto getById(@PathVariable("id") String id) {
        TdrsRule tdrsRule = repository.findOne(id);
        return convertToFullModel(tdrsRule);
    }

    @ApiOperation(value = "Load a Tdrs Rule list by Tdrs Rule IDs", response = TdrsRule[].class, httpMethod = "GET")
    @RequestMapping(value = "listByIds", method = RequestMethod.GET)
    public List<TdrsRuleDto> tdrsRulesById(@RequestParam("id") List<String> ids) {
        return convertListToFullModel(repository.findAll(new SearchCriteriaList(new SearchIdInCriteria(ids))));
    }

    @ApiOperation(value = "Insert a new Tdrs Rule", response = TdrsRule.class, httpMethod = "POST")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public TdrsRuleDto insert(@RequestBody @Valid TdrsRuleDto entity, HttpServletRequest request) throws IOException {
        TdrsRule tdrsRule = save(entity, RestUtils.getUserId(request), RepositoryDecorator.WriteMode.INSERT);
        return convertToFullModel(tdrsRule);
    }

    @ApiOperation(value = "Insert multiple Tdrs Rules provided as short DTO", response = TdrsRule.class, httpMethod = "POST")
    @RequestMapping(value = "list/short", method = RequestMethod.POST)
    public Iterable<TdrsRule> insertAllShort(@RequestBody @Valid List<TdrsRule> entities, HttpServletRequest request) throws IOException {
        return repository.bulkSaveWithoutHistory(entities, RestUtils.getUserId(request));
    }


    @ApiOperation(value = "Update a Tdrs Rule", response = TdrsRule.class, httpMethod = "PUT")
    @RequestMapping(value = {"", "{id}"}, method = RequestMethod.PUT)
    public TdrsRuleDto update(@RequestBody @Valid TdrsRuleDto entity, HttpServletRequest request) {
        TdrsRule tdrsRule = save(entity, RestUtils.getUserId(request), RepositoryDecorator.WriteMode.UPDATE);
        return convertToFullModel(tdrsRule);
    }

    @ApiOperation(value = "Delete a Tdrs Rule", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable("id") String id, HttpServletRequest request) {
        repository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all Tdrs Rules", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        repository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by Tdrs Rule id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repository.getHistoryById(id);
    }

    private TdrsRule save(TdrsRuleDto tdrsRuleDto, String userId, RepositoryDecorator.WriteMode writeMode) {
        if (tdrsRuleDto.getId() == null) {
            tdrsRuleDto.setId(UUID.randomUUID().toString());
        }
        TdrsRule tdrsRule = TdrsRule.fromDto(tdrsRuleDto);
        RecommendationNode recommendationNode = tdrsRecommendationTreeMapper.toFlatRecommendation(tdrsRule.getRecommendation());
        tdrsRule.setRecommendation(recommendationNode);
        return repository.save(tdrsRule, userId, writeMode);
    }

    private TdrsRuleDto convertToFullModel(TdrsRule tdrsRule) {
        logger.debug(String.format("Loading dependencies for TDRS Rule ID=%s, NAME='%s'", tdrsRule.getId(), tdrsRule.getName()));
        TdrsRuleDto converted = tdrsRule.toDto();
        if (converted.getRecommendation() != null) {
            logger.debug(String.format("Loading documents for TDRS Rule ID=%s, NAME='%s'", tdrsRule.getId(), tdrsRule.getName()));
            RecommendationNode recommendationNode = tdrsRecommendationTreeMapper.toFullRecommendation(converted.getRecommendation());
            converted.setRecommendation(recommendationNode);
        }
        if (tdrsRule.getDestinationCountryId() != null) {
            logger.debug(String.format("Loading destination countries for TDRS Rule ID=%s, NAME='%s'", tdrsRule.getId(), tdrsRule.getName()));
            converted.setDestinationCountry(countryMapper.idToDto(tdrsRule.getDestinationCountryId()));
        }
        if (tdrsRule.getCitizenshipCountryIds() != null) {
            logger.debug(String.format("Loading citizenship countries for TDRS Rule ID=%s, NAME='%s'", tdrsRule.getId(), tdrsRule.getName()));
            converted.setCitizenshipCountries(countryMapper.idsToDtos(tdrsRule.getCitizenshipCountryIds()));
        }
        return converted;
    }

    private List<TdrsRuleDto> convertListToFullModel(List<TdrsRule> tdrsRules) {
        if (tdrsRules == null) {
            return null;
        }
        return tdrsRules.stream().map(this::convertToFullModel).collect(Collectors.toList());
    }

    private SearchCriteriaList getSearchCriterias(String name, String destinationCountryId, List<String> destinationCountryCodes) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name)),
                new SearchEqualCriteria<>(DESTINATION_COUNTRY_ID, destinationCountryId),
                new SearchFieldInCriteria(DESTINATION_COUNTRY_ID, countryMapper.codesToIds(destinationCountryCodes)));
    }

}
