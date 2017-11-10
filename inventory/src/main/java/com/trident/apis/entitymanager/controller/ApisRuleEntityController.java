package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.apis.entitymanager.model.Port;
import com.trident.apis.entitymanager.model.TridentToPortTranslation;
import com.trident.shared.immigration.dto.apis.ApisRuleDto;
import com.trident.shared.immigration.dto.apis.ApisTemplateDto;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortTranslationDto;
import com.trident.shared.immigration.dto.apis.port.PortDto;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import com.trident.shared.immigration.repository.criteria.SearchLikeCriteria;
import com.trident.shared.immigration.util.FutureUtil;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1/apis/rules", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "api/v1/apis/rules", description = "CRUD operations with APIS Rules")
public class ApisRuleEntityController {

    @Autowired
    private RepositoryDecorator<ApisRule> apisRuleRepository;
    @Autowired
    private RepositoryDecorator<Port> portRepository;
    @Autowired
    private RepositoryDecorator<ApisTemplate> templateRepository;
    @Autowired
    private RepositoryDecorator<TridentToPortTranslation> trident2portRepository;
    @Autowired
    @Lazy
    private Mapper mapper;

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads APIS Rule list", response = ApisRule[].class, httpMethod = "GET")
    @GetMapping
    public Iterable<ApisRule> getAll(
            Sort sort,
            @ApiParam(required = false, name = NAME, value = FILTER_BY_NAME) @RequestParam(required = false) String name) {
        return apisRuleRepository.findAll(
                sort, new SearchCriteriaList(new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name))));
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads APIS Rule list by page", response = Page.class, httpMethod = "GET")
    @RequestMapping(value = "page")
    public Page<ApisRule> getPage(
            Pageable pageable,
            @ApiParam(required = false, name = NAME, value = FILTER_BY_NAME) @RequestParam(required = false) String name) {
        return apisRuleRepository.findAll(pageable, new SearchCriteriaList(new SearchLikeCriteria(NAME, name)));
    }

    @ApiOperation(value = "Load APIS Rule by ID", response = ApisRule.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public ApisRuleDto byId(@PathVariable(name = "id") String id) {
        ApisRule apisRule = apisRuleRepository.findOne(id);
        return loadDependencies(apisRule);
    }

    private ApisRuleDto loadDependencies(ApisRule apisRule) {
        List<CompletableFuture<ApisTemplate>> templateFutures = apisRule.getApisTemplateIds().stream().map(id -> templateRepository.findOneAsync(id)).collect(Collectors.toList());
        List<CompletableFuture<Port>> portFutures = apisRule.getApisPortIds().stream().map(id -> portRepository.findOneAsync(id)).collect(Collectors.toList());
        CompletableFuture<TridentToPortTranslation> trident2port = trident2portRepository.findOneAsync(apisRule.getTridentToPortTranslationId());
        ApisRuleDto result = mapper.map(apisRule, ApisRuleDto.class);
        result.setApisPorts(
                portFutures.stream()
                        .map(FutureUtil::get)
                        .map(port -> mapper.map(port, PortDto.class))
                        .collect(Collectors.toList()));
        result.setApisTemplates(
                templateFutures.stream()
                        .map(FutureUtil::get)
                        .map(template -> mapper.map(template, ApisTemplateDto.class))
                        .collect(Collectors.toList()));
        result.setTridentToPortTranslation(
                mapper.map(FutureUtil.get(trident2port), TridentToPortTranslationDto.class));
        return result;
    }

    @ApiOperation(value = "Load APIS Rules array by port ID")
    @GetMapping(value = "byPortId/{portId}")
    public Iterable<ApisRule> byPortId(@PathVariable("portId") String portId) {
        return apisRuleRepository.findAll(new SearchCriteriaList(new SearchInArrayFieldCriteria<String>("apisPortIds", portId)));
    }

    @ApiOperation(value = "Save new APIS Rule", response = ApisRule.class, httpMethod = "POST")
    @RequestMapping(method = RequestMethod.POST)
    public ApisRuleDto insert(@RequestBody @Valid ApisRule apisRule, HttpServletRequest request) {
        checkIfIdSet(apisRule);
        return loadDependencies(apisRuleRepository.insert(apisRule, RestUtils.getUserId(request)));
    }

    @ApiOperation(value = "Save all new APIS Rules", response = ApisRule.class, httpMethod = "POST")
    @RequestMapping(path = "list", method = RequestMethod.POST)
    public List<ApisRule> insertAll(@RequestBody @Valid List<ApisRule> apisRules, HttpServletRequest request) {
        checkIfIdSet(apisRules);
        return apisRuleRepository.bulkSaveWithoutHistory(apisRules, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update APIS Rule", response = ApisRule.class, httpMethod = "PUT")
    @RequestMapping(method = RequestMethod.PUT, value = {"", "{id}"})
    public ApisRuleDto update(@RequestBody @Valid ApisRule apisRule, HttpServletRequest request) {
        checkIfIdSet(apisRule);
        return loadDependencies(apisRuleRepository.update(apisRule, RestUtils.getUserId(request)));
    }

    @ApiOperation(value = "Delete APIS Rule by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable(value = "id") String id, HttpServletRequest request) {
        apisRuleRepository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by ApisRuleDto id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return apisRuleRepository.getHistoryById(id);
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        apisRuleRepository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    private void checkIfIdSet(List<ApisRule> apisRules) {
        apisRules.forEach(this::checkIfIdSet);
    }

    private void checkIfIdSet(ApisRule apisRule) {
        if (apisRule.getId() == null) {
            apisRule.setId(UUID.randomUUID().toString());
        }
    }

}
