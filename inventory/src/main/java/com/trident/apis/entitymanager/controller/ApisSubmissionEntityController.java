package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.ApisSubmission;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.errorhandling.GlobalExceptionHandler;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchBetweenCriteria;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.apache.log4j.Logger;
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
@RequestMapping(path = "/api/v1/apis/submissions", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "api/v1/apis/submission", description = "CRUD operations with APIS Submissions")
public class ApisSubmissionEntityController {

    private static final Logger logger = Logger.getLogger(ApisSubmissionEntityController.class);
    private static final String DEFAULT_IS_FULL_MODEL = "false";

    @Autowired
    private RepositoryDecorator<ApisSubmission> repositoryDecorator;
    @Autowired
    private Environment environment;

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION),
            @ApiImplicitParam(name = STATUS, value = FILTER_BY_STATUS, dataType = STRING, paramType = QUERY_PARAM_TYPE),
            @ApiImplicitParam(value = FILTER_BY_PORT_ID, name = PORT_ID),
            @ApiImplicitParam(value = FILTER_BY_DATE, name = PARAM_DATE_FROM + ", " + PARAM_DATE_TO),
            @ApiImplicitParam(name = PARAM_FULL_MODEL, dataType = BOOLEAN, paramType = QUERY_PARAM_TYPE,
                    value = DESCRIPTION_IS_FULL_MODEL, defaultValue = DEFAULT_IS_FULL_MODEL)
    })
    @ApiOperation(value = "Loads APIS Submission list", response = ApisSubmission[].class, httpMethod = "GET")
    @RequestMapping
    public Iterable<ApisSubmission> getAll(
            Sort sort,
            @ApiParam(value = FILTER_BY_BRAND_CODE, name = BRAND_CODE) @RequestParam(required = false) String brandCode,
            @ApiParam(value = FILTER_BY_SHIP_TRIDENT_CODE, name = SHIP_CODE) @RequestParam(required = false) String shipCode,
            @ApiParam(value = FILTER_BY_RULE_ID, name = PARAM_RULE_ID) @RequestParam(required = false) String ruleId,
            @ApiParam(name = STATUS, value = FILTER_BY_STATUS) @RequestParam(required = false) String status,
            @ApiParam(name = PORT_ID, value = FILTER_BY_PORT_ID) @RequestParam(required = false) String portId,
            @ApiParam(value = FILTER_BY_CVI_ENTRY, name = CVI_ENTRY_ID) String cviEntryId,
            @ApiParam(name = PARAM_DATE_FROM, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateFrom,
            @ApiParam(name = PARAM_DATE_TO, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateTo,
            @ApiParam(name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL, defaultValue = DEFAULT_IS_FULL_MODEL)
                @RequestParam(name = PARAM_FULL_MODEL, defaultValue = DEFAULT_IS_FULL_MODEL) Boolean isFullModel) {
        SearchCriteriaList criterias = getSearchCriteria(status, portId, dateFrom, dateTo, brandCode, shipCode, ruleId, cviEntryId);
        return repositoryDecorator.findAll(isFullModel ? null : getShortModelFields(), sort, criterias);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Loads APIS Submission list by page", response = Page.class, httpMethod = "GET")
    @RequestMapping(value = "page")
    public Page<ApisSubmission> getPage(
            Pageable pageable,
            @ApiParam(value = FILTER_BY_BRAND_CODE, name = BRAND_CODE) @RequestParam(required = false) String brandCode,
            @ApiParam(value = FILTER_BY_SHIP_TRIDENT_CODE, name = SHIP_CODE) @RequestParam(required = false) String shipCode,
            @ApiParam(value = FILTER_BY_RULE_ID, name = PARAM_RULE_ID) @RequestParam(required = false) String ruleId,
            @ApiParam(value = FILTER_BY_CVI_ENTRY, name = CVI_ENTRY_ID) @RequestParam(required = false) String cviEntryId,
            @ApiParam(name = STATUS, value = FILTER_BY_STATUS) @RequestParam(required = false) String status,
            @ApiParam(name = PORT_ID, value = FILTER_BY_PORT_ID) @RequestParam(required = false) String portId,
            @ApiParam(name = PARAM_DATE_FROM, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateFrom,
            @ApiParam(name = PARAM_DATE_TO, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateTo,
            @ApiParam(name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL, defaultValue = DEFAULT_IS_FULL_MODEL)
                @RequestParam(name = PARAM_FULL_MODEL, defaultValue = DEFAULT_IS_FULL_MODEL) Boolean isFullModel) {
        SearchCriteriaList criterias = getSearchCriteria(status, portId, dateFrom, dateTo, brandCode, shipCode, ruleId, cviEntryId);
        return repositoryDecorator.findAll(isFullModel ? null : getShortModelFields(), pageable, criterias);
    }

    @ApiOperation(value = "Load APIS Submission by ID", response = ApisSubmission.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public ApisSubmission byId(@PathVariable(name = "id") String id) {
        return repositoryDecorator.findOne(id);
    }

    @ApiOperation(value = "Save new APIS Submission", response = ApisSubmission.class, httpMethod = "POST")
    @RequestMapping(method = RequestMethod.POST)
    public ApisSubmission insert(@RequestBody @Valid ApisSubmission apisSubmission, HttpServletRequest request) {
        checkIfIdSet(apisSubmission);
        return repositoryDecorator.insert(apisSubmission, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update APIS Submission", response = ApisSubmission.class, httpMethod = "PUT")
    @PutMapping(path = {"","{id}"})
    public ApisSubmission update(
            @RequestBody @Valid ApisSubmission apisSubmission,
            HttpServletRequest request,
            @RequestParam(name = "ignoreCAS", defaultValue = "false") boolean ignoreCAS) {
        try {
            checkIfIdSet(apisSubmission);
            if (!ignoreCAS) {
                return repositoryDecorator.update(apisSubmission, RestUtils.getUserId(request));
            } else {
                return repositoryDecorator.save(apisSubmission, RestUtils.getUserId(request));
            }
        } catch (Exception e) {
            logger.error("Failed to update submission", e);
            throw new RuntimeException(e);
        }
    }

    @ApiOperation(value = "Delete APIS Submission by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable(value = "id") String id, HttpServletRequest request) {
        repositoryDecorator.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all submissions", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<SimpleResponse> deleteAll(HttpServletRequest request) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(ControllerConstants.PROD_ENVIRONMENT_NAME::equalsIgnoreCase);
        if (!isProd) {
            repositoryDecorator.deleteAll(RestUtils.getUserId(request));
            return new ResponseEntity<>(new SimpleResponse("OK"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SimpleResponse("Not allowed on production."), HttpStatus.FORBIDDEN);
        }
    }

    @ApiOperation(value = "Returns history by ApisSubmission id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repositoryDecorator.getHistoryById(id);
    }

    private void checkIfIdSet(ApisSubmission apisSubmission) {
        if (apisSubmission.getId() == null) {
            apisSubmission.setId(UUID.randomUUID().toString());
        }
    }

    private SearchCriteriaList getSearchCriteria(String status,
                                                 String portId,
                                                 Long dateFrom,
                                                 Long dateTo,
                                                 String brandCode,
                                                 String shipCode,
                                                 String ruleId,
                                                 String cviEntryId) {
        return new SearchCriteriaList(
                new SearchEqualCriteria<>(ControllerConstants.STATUS, status),
                new SearchEqualCriteria<>(ControllerConstants.PORT_ID, portId),
                new SearchBetweenCriteria<>(ControllerConstants.FIELD_START_DATE, dateFrom, dateTo),
                new SearchEqualCriteria<>("cruiseVoyageItineraryDto.brandCode", brandCode),
                new SearchEqualCriteria<>("cruiseVoyageItineraryDto.shipCode", shipCode),
                new SearchEqualCriteria<>(ControllerConstants.PARAM_RULE_ID, ruleId),
                new SearchEqualCriteria<>("cruiseVoyageItineraryEntryId", cviEntryId)
        );
    }

    private List<String> getShortModelFields() {
        return ImmutableList.of("ruleId", "ruleName", "portId", "portName", "direction", "ruleType", "noticeTransactionType",
                "startDate", "statusDate", "errors", "status", "submissionMethod", "trackingReference", "cruiseVoyageItineraryEntryId");
    }

}
