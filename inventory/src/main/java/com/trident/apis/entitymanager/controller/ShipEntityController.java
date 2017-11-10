package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.apis.entitymanager.repository.ShipCouchbaseRepository;
import com.trident.apis.entitymanager.service.ShipMapper;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipBuildInfo;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipLegal;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipTechnicalSpec;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.criteria.SearchCriteria;
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
import java.util.stream.Collectors;

import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1/apis/ships", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "api/v1/apis/ships", description = "CRUD operations with Ship Configuration")
public class ShipEntityController {

    public static final String SHIP_IDENTITY_NAME = "ship.shipIdentity.name";
    public static final String SHIP_BRAND_CODE = "ship.brandCode";
    public static final String SHIP_SHIP_TRIDENT_CODE = "ship.shipTridentCode";

    @Autowired
    private ShipCouchbaseRepository shipRepository;
    @Autowired
    private ShipMapper shipMapper;
    @Autowired
    private Environment environment;

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads Ship Configuration list", response = ShipEntity[].class, httpMethod = "GET")
    @GetMapping
    public Iterable<ShipEntity> all(
            Sort sort,
            @ApiParam(name = NAME, value = FILTER_BY_NAME)
            @RequestParam(required = false)
                    String name,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(required = false)
                    String brandCode,
            @ApiParam(name = SHIP_TRIDENT_CODE, value = FILTER_BY_SHIP_TRIDENT_CODE)
            @RequestParam(required = false)
                    String shipTridentCode,
            @ApiParam(name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL)
            @RequestParam(name = PARAM_FULL_MODEL, required = false, defaultValue = "false")
                    Boolean isFullModel) {
        return shipRepository.findAll(
                sort,
                getCriteria(name, brandCode, shipTridentCode));
    }

    @ApiImplicitParams({@ApiImplicitParam(name = NAME, dataType = STRING, paramType = QUERY_PARAM_TYPE, required = false)})
    @ApiOperation(value = "Loads Ship Configuration list by page", response = Page.class, httpMethod = "GET")
    @GetMapping(value = "page")
    public Page<ShipEntity> allByPage(
            Pageable pageable,
            @ApiParam(name = NAME, value = FILTER_BY_NAME)
            @RequestParam(required = false)
                    String name,
            @ApiParam(name = BRAND_CODE, value = FILTER_BY_BRAND_CODE)
            @RequestParam(required = false)
                    String brandCode,
            @ApiParam(name = SHIP_TRIDENT_CODE, value = FILTER_BY_SHIP_TRIDENT_CODE)
            @RequestParam(required = false)
                    String shipTridentCode,
            @ApiParam(name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL)
            @RequestParam(name = PARAM_FULL_MODEL, required = false, defaultValue = "false")
                    Boolean isFullModel) {
        if (isFullModel) {
            return shipRepository.findAllFull(
                    pageable,
                    getCriteria(name, brandCode, shipTridentCode));
        } else {
            return shipRepository.findAll(
                    pageable,
                    getCriteria(name, brandCode, shipTridentCode));
        }
    }

    private SearchCriteriaList getCriteria(String name, String brandCode, String shipTridentCode) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(SHIP_IDENTITY_NAME, ImmigrationStringUtils.decodeParam(name)),
                new SearchLikeCriteria(SHIP_BRAND_CODE, ImmigrationStringUtils.decodeParam(brandCode)),
                new SearchLikeCriteria(SHIP_SHIP_TRIDENT_CODE, ImmigrationStringUtils.decodeParam(shipTridentCode))
        );
    }

    @ApiOperation(value = "Load Ship Configuration by ID", response = ShipDto.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public ShipDto byId(
            @PathVariable(name = "id")
                    String id) {
        return shipRepository.findOneFull(id);
    }

    @ApiOperation(value = "Save new Ship Configuration", response = ShipDto.class, httpMethod = "POST")
    @RequestMapping(method = RequestMethod.POST)
    public ShipDto insert(@RequestBody @Valid ShipEntity shipEntity, HttpServletRequest request) {
        checkIfIdSet(shipEntity);
        cleanUpReferencedEntities(shipEntity);
        ShipEntity inserted = shipRepository.insert(shipEntity, RestUtils.getUserId(request));
        return shipRepository.findOneFull(inserted.getId());
    }

    private void cleanUpReferencedEntities(ShipEntity shipEntity) {
        ShipTechnicalSpec shipTechnicalSpec = shipEntity.getShip().getShipTechnicalSpec();
        if (shipTechnicalSpec != null){
            ShipBuildInfo shipBuildInfo = shipTechnicalSpec.getShipBuildInfo();
            if (shipBuildInfo != null) {
                if (shipBuildInfo.getManufacturer() != null ){
                    shipBuildInfo.setManufacturerId(shipBuildInfo.getManufacturer().getId());
                    shipBuildInfo.setManufacturer(null);
                }
                if (shipBuildInfo.getBuiltAtCountry() != null) {
                    shipBuildInfo.setBuiltAtCountryId(shipBuildInfo.getBuiltAtCountry().getId());
                    shipBuildInfo.setBuiltAtCountry(null);
                }
            }
        }
        ShipLegal shipLegal = shipEntity.getShip().getShipLegal();
        if (shipLegal != null) {
            if (shipLegal.getAgent() != null) {
                shipLegal.setAgentId(shipLegal.getAgent().getId());
                shipLegal.setAgent(null);
            }
            if (shipLegal.getOperator() != null) {
                shipLegal.setOperatorId(shipLegal.getOperator().getId());
                shipLegal.setOperator(null);
            }
            if (shipLegal.getCharterer() != null) {
                shipLegal.setChartererId(shipLegal.getCharterer().getId());
                shipLegal.setCharterer(null);
            }
            if (shipLegal.getOwner() != null) {
                shipLegal.setOwnerId(shipLegal.getOwner().getId());
                shipLegal.setOwner(null);
            }
            if (shipLegal.getNationalityCountry() != null) {
                shipLegal.setNationalityCountryId(shipLegal.getNationalityCountry().getId());
                shipLegal.setNationalityCountry(null);
            }
        }
    }

    @ApiOperation(value = "Save all new Ship Configurations", response = ShipDto.class, httpMethod = "POST")
    @RequestMapping(path = "list", method = RequestMethod.POST)
    public List<ShipEntity> insertAll(@RequestBody @Valid List<ShipEntity> shipEntities, HttpServletRequest request) {
        checkIfIdSet(shipEntities);
        return shipRepository.bulkSaveWithoutHistory(shipEntities, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update Ship Configuration", response = ShipDto.class, httpMethod = "PUT")
    @RequestMapping(method = RequestMethod.PUT, value = {"", "{id}"})
    public ShipDto update(@RequestBody @Valid ShipEntity shipEntity, HttpServletRequest request) {
        checkIfIdSet(shipEntity);
        ShipEntity updated = shipRepository.update(shipEntity, RestUtils.getUserId(request));
        return shipRepository.findOneFull(updated.getId());
    }

    @ApiOperation(value = "Delete Ship Configuration by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable(value = "id") String id, HttpServletRequest request) {
        shipRepository.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<SimpleResponse> deleteAll(HttpServletRequest request) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles()).anyMatch(ControllerConstants.PROD_ENVIRONMENT_NAME::equalsIgnoreCase);
        if (!isProd) {
            shipRepository.deleteAll(RestUtils.getUserId(request));
            return new ResponseEntity<>(new SimpleResponse("OK"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SimpleResponse("Not allowed on production."), HttpStatus.FORBIDDEN);
        }
    }

    @ApiOperation(value = "Returns history by ShipEntity id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return shipRepository.getHistoryById(id);
    }

    private void checkIfIdSet(List<ShipEntity> shipEntities) {
        shipEntities.forEach(this::checkIfIdSet);
    }

    private void checkIfIdSet(ShipEntity shipEntity) {
        if (shipEntity.getId() == null) {
            shipEntity.setId(UUID.randomUUID().toString());
        }
    }

}
