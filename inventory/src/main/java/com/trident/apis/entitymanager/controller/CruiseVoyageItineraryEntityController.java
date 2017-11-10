package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.repository.CruiseVoyageItineraryCouchbaseRepository;
import com.trident.apis.entitymanager.service.cvi.CruiseVoyageItineraryMapper;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.dto.apis.cvi.CviPortEntry;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.criteria.*;
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
@RequestMapping(path = "/api/v1/apis/cvi", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/apis/cvi", description = "CRUD operations with CVI Configuration")
public class CruiseVoyageItineraryEntityController {

    @Autowired
    private CruiseVoyageItineraryCouchbaseRepository cviRepository;
    @Autowired
    private CruiseVoyageItineraryMapper cviMapper;
    @Autowired
    private Environment environment;

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads CVI Configuration list", response = CruiseVoyageItineraryDto[].class, httpMethod = "GET")
    @GetMapping
    public Iterable<CruiseVoyageItinerary> all(
            Sort sort,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(value = BRAND_CODE, required = false)
                    String brandCode,
            @ApiParam(name = SHIP_CODE, value = FILTER_BY_SHIP_TRIDENT_CODE)
            @RequestParam(value = SHIP_CODE, required = false)
                    String shipCode,
            @ApiParam(name = START_DATE, value = FILTER_BY_START_DATE)
            @RequestParam(value = START_DATE, required = false)
                    Long startDate,
            @ApiParam(name = END_DATE, value = FILTER_BY_END_DATE)
            @RequestParam(value = END_DATE, required = false)
                    Long endDate,
            @ApiParam(name = PORT_ID, value = FILTER_BY_PORT_ID)
            @RequestParam(value = PORT_ID, required = false)
                    String portId) {
        return cviRepository.findAll(
                sort,
                getCriteriaInDateRange(shipCode, brandCode, startDate, endDate, portId));
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads CVI Configuration list by page", response = Page.class, httpMethod = "GET")
    @GetMapping(value = "page")
    public Page<CruiseVoyageItinerary> allByPage(
            Pageable pageable,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(value = BRAND_CODE, required = false)
                    String brandCode,
            @ApiParam(name = SHIP_CODE, value = FILTER_BY_SHIP_TRIDENT_CODE)
            @RequestParam(value = SHIP_CODE, required = false)
                    String shipCode,
            @ApiParam(name = START_DATE, value = FILTER_BY_START_DATE)
            @RequestParam(value = START_DATE, required = false)
                    Long startDate,
            @ApiParam(name = END_DATE, value = FILTER_BY_END_DATE)
            @RequestParam(value = END_DATE, required = false)
                    Long endDate,
            @ApiParam(name = PORT_ID, value = FILTER_BY_PORT_ID)
            @RequestParam(value = PORT_ID, required = false)
                    String portId) {
        return cviRepository.findAll(
                pageable,
                getCriteriaInDateRange(shipCode, brandCode, startDate, endDate, portId));
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads CVI Configuration list by page", response = Page.class, httpMethod = "GET")
    @GetMapping(value = "containing-date")
    public List<CruiseVoyageItinerary> allContainingDate(
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(value = BRAND_CODE, required = false)
                    String brandCode,
            @ApiParam(name = SHIP_CODE, value = FILTER_BY_SHIP_TRIDENT_CODE)
            @RequestParam(value = SHIP_CODE, required = false)
                    String shipCode,
            @ApiParam(name = DATE, value = FILTER_CONTAINING_DATE)
            @RequestParam(value = DATE, required = false)
                    Long date,
            @ApiParam(name = PORT_ID, value = FILTER_BY_PORT_ID)
            @RequestParam(value = PORT_ID, required = false)
                    String portId) {
        return cviRepository.findAll(
                new Sort(new Sort.Order(Sort.Direction.ASC, START_DATE)),
                getCriteriaContainingDate(shipCode, brandCode, date, portId));
    }

    private SearchCriteriaList getCriteriaInDateRange(String shipCode, String brandCode, Long startDate, Long endDate, String portId) {
        return new SearchCriteriaList(
                new SearchLikeCriteria<>(SHIP_CODE, ImmigrationStringUtils.decodeParam(shipCode)),
                new SearchLikeCriteria<>(BRAND_CODE, ImmigrationStringUtils.decodeParam(brandCode)),
                new SearchMoreCriteria<>(START_DATE, startDate),
                new SearchLessCriteria<>(END_DATE, endDate),
                new ArrayContainsObjectWithFieldSpecifiedCriteria<>("cviPortEntries", "portId", portId)
        );
    }

    private SearchCriteriaList getCriteriaContainingDate(String shipCode, String brandCode, Long date, String portId) {
        return new SearchCriteriaList(
                new SearchLikeCriteria<>(SHIP_CODE, ImmigrationStringUtils.decodeParam(shipCode)),
                new SearchLikeCriteria<>(BRAND_CODE, ImmigrationStringUtils.decodeParam(brandCode)),
                new SearchLessCriteria<>(START_DATE, date),
                new SearchMoreCriteria<>(END_DATE, date),
                new ArrayContainsObjectWithFieldSpecifiedCriteria<>("cviPortEntries", "portId", portId)
        );
    }

    @ApiOperation(value = "Load CVI Configuration by ID", response = CruiseVoyageItineraryDto.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public CruiseVoyageItineraryDto byId(
            @PathVariable(name = "id")
                    String id) {
        return cviRepository.findOneFull(id);
    }

    @ApiOperation(value = "Save new CVI Configuration", response = CruiseVoyageItineraryDto.class, httpMethod = "POST")
    @RequestMapping(method = RequestMethod.POST)
    public CruiseVoyageItineraryDto insert(@RequestBody @Valid CruiseVoyageItineraryDto cviDto, HttpServletRequest request) {
        CruiseVoyageItinerary cvi = cviMapper.fromCviDto(cviDto);
        checkIfIdSet(cvi);
        checkIfIdSetForCviPortEntry(cvi.getCviPortEntries());
        clearReferencedObjects(cvi);
        populateCviDates(cvi);
        CruiseVoyageItinerary inserted = cviRepository.insert(cvi, RestUtils.getUserId(request));
        return cviRepository.findOneFull(inserted.getId());
    }

    private void clearReferencedObjects(CruiseVoyageItinerary cvi) {
        cvi.getCviPortEntries().forEach(cviPortEntry -> {
            if (cviPortEntry.getPort() != null) {
                cviPortEntry.setPortId(cviPortEntry.getPort().getId());
                cviPortEntry.setPort(null);
            }
        });
    }

    private void populateCviDates(CruiseVoyageItinerary cvi) {
        List<CviPortEntry> entries = cvi.getCviPortEntries();
        cvi.setStartDate(entries.get(0).getDepartureDate());
        cvi.setEndDate(entries.get(entries.size()-1).getArrivalDate());
    }

    @ApiOperation(value = "Save new CVI Configuration", httpMethod = "POST")
    @RequestMapping(path = "list", method = RequestMethod.POST)
    public List<CruiseVoyageItinerary> insertAll(@RequestBody @Valid List<CruiseVoyageItinerary> cviList, HttpServletRequest request) {
        checkIfIdSet(cviList);
        cviList.forEach(this::populateCviDates);
        return cviRepository.bulkSaveWithoutHistory(cviList, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update CVI Configuration", response = CruiseVoyageItineraryDto.class, httpMethod = "PUT")
    @RequestMapping(method = RequestMethod.PUT, value = {"", "{id}"})
    public CruiseVoyageItineraryDto update(@RequestBody @Valid CruiseVoyageItineraryDto cviDto, HttpServletRequest request) {
        CruiseVoyageItinerary cvi = cviMapper.fromCviDto(cviDto);
        clearReferencedObjects(cvi);
        checkIfIdSet(cvi);
        checkIfIdSetForCviPortEntry(cvi.getCviPortEntries());
        populateCviDates(cvi);
        CruiseVoyageItinerary updated = cviRepository.update(cvi, RestUtils.getUserId(request));
        return cviRepository.findOneFull(updated.getId());
    }

    @ApiOperation(value = "Delete CVI Configuration by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable(value = "id") String id, HttpServletRequest request) {
        cviRepository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<SimpleResponse> deleteAll(HttpServletRequest request) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(ControllerConstants.PROD_ENVIRONMENT_NAME::equalsIgnoreCase);
        if (!isProd) {
            cviRepository.deleteAll(RestUtils.getUserId(request));
            return new ResponseEntity<>(new SimpleResponse("OK"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SimpleResponse("Not allowed on production."), HttpStatus.FORBIDDEN);
        }
    }

    @ApiOperation(value = "Returns history by CVI id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return cviRepository.getHistoryById(id);
    }

    private void checkIfIdSet(List<CruiseVoyageItinerary> cruiseVoyageItineraryList) {
        cruiseVoyageItineraryList.forEach(this::checkIfIdSet);
    }

    private void checkIfIdSet(CruiseVoyageItinerary cruiseVoyageItinerary) {
        if (cruiseVoyageItinerary.getId() == null) {
            cruiseVoyageItinerary.setId(UUID.randomUUID().toString());
        }
    }

    private void checkIfIdSetForCviPortEntry(List<CviPortEntry> cviPortEntries) {
        cviPortEntries.forEach(this::checkIfIdSet);
    }

    private void checkIfIdSet(CviPortEntry cviPortEntry) {
        if(cviPortEntry.getId() == null){
            cviPortEntry.setId(UUID.randomUUID().toString());
        }
    }
}
