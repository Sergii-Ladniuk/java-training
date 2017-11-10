package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.CompanyEntity;
import com.trident.apis.entitymanager.repository.CompanyCouchbaseRepository;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchLikeCriteria;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1/apis/companies", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "api/v1/apis/companies", description = "CRUD operations with Company Configuration")
public class CompanyEntityController {

    @Autowired
    private Environment environment;

    private CompanyCouchbaseRepository companyCouchbaseRepository;
    @Autowired
    public void setCompanyCouchbaseRepository(CompanyCouchbaseRepository companyCouchbaseRepository) {
        this.companyCouchbaseRepository = companyCouchbaseRepository;
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads Company Configuration list", response = CompanyEntity[].class, httpMethod = "GET")
    @GetMapping
    public Iterable<CompanyEntity> all(
            Sort sort,
            @ApiParam(name = NAME, value = FILTER_BY_NAME)
            @RequestParam(required = false)
                    String name,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(required = false)
                    String brandCode) {
        return companyCouchbaseRepository.findAll(
                sort,
                getCriteria(name, brandCode));
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads Company Configuration list by page", response = Page.class, httpMethod = "GET")
    @GetMapping(value = "page")
    public Page<CompanyEntity> allByPage(
            Pageable pageable,
            @ApiParam(name = NAME, value = FILTER_BY_NAME)
            @RequestParam(required = false)
                    String name,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(required = false)
                    String brandCode) {
        return companyCouchbaseRepository.findAll(
                pageable,
                getCriteria(name, brandCode));
    }

    private SearchCriteriaList getCriteria(String name, String brandCode) {
        return new SearchCriteriaList(
                new SearchLikeCriteria("company.name", ImmigrationStringUtils.decodeParam(name)),
                new SearchLikeCriteria("company.brandCode", ImmigrationStringUtils.decodeParam(brandCode))
        );
    }

    @ApiOperation(value = "Load Company Configuration by ID", response = CompanyEntity.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public CompanyEntity byId(
            @PathVariable(name = "id")
                    String id,
            @ApiParam(name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL)
            @RequestParam(name = PARAM_FULL_MODEL, required = false, defaultValue = "true")
                    Boolean isFullModel) {
        return companyCouchbaseRepository.findOne(id);
    }

    @ApiOperation(value = "Save new Company Configuration", response = CompanyEntity.class, httpMethod = "POST")
    @RequestMapping(method = RequestMethod.POST)
    public CompanyEntity insert(@RequestBody @Valid CompanyEntity companyEntity, HttpServletRequest request) {
        checkIfIdSet(companyEntity);
        return companyCouchbaseRepository.insert(companyEntity, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save all new Company Configurations", response = CompanyEntity.class, httpMethod = "POST")
    @RequestMapping(path = "list", method = RequestMethod.POST)
    public List<CompanyEntity> insertAll(@RequestBody @Valid List<CompanyEntity> companyEntity, HttpServletRequest request) {
        checkIfIdSet(companyEntity);
        return companyCouchbaseRepository.bulkSaveWithoutHistory(companyEntity, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update Company Configuration", response = CompanyEntity.class, httpMethod = "PUT")
    @RequestMapping(method = RequestMethod.PUT, value = {"", "{id}"})
    public CompanyEntity update(@RequestBody @Valid CompanyEntity companyEntity, HttpServletRequest request) {
        checkIfIdSet(companyEntity);
        return companyCouchbaseRepository.update(companyEntity, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete Company Configuration by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable(value = "id") String id, HttpServletRequest request) {
        companyCouchbaseRepository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<SimpleResponse> deleteAll(HttpServletRequest request) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(ControllerConstants.PROD_ENVIRONMENT_NAME::equalsIgnoreCase);
        if (!isProd) {
            companyCouchbaseRepository.deleteAll(RestUtils.getUserId(request));
            return new ResponseEntity<>(new SimpleResponse("OK"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SimpleResponse("Not allowed on production."), HttpStatus.FORBIDDEN);
        }
    }

    @ApiOperation(value = "Returns history by CompanyEntity id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return companyCouchbaseRepository.getHistoryById(id);
    }

    private void checkIfIdSet(List<CompanyEntity> companyEntities) {
        companyEntities.forEach(this::checkIfIdSet);
    }

    private void checkIfIdSet(CompanyEntity companyEntity) {
        if (companyEntity.getId() == null) {
            companyEntity.setId(UUID.randomUUID().toString());
        }
    }

}
