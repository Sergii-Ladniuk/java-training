package com.trident.apis.entitymanager.repository;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ShipDtoForLimitedQuery;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.shared.immigration.dto.apis.knowledge.legal.CompanyDto;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.dto.apis.port.CountryDto;
import com.trident.shared.immigration.exception.PersistenceException;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.RepositoryPagingAndSortService;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShipCouchbaseRepository extends RepositoryDecorator<ShipEntity> {

    private static final Logger logger = Logger.getLogger(ShipCouchbaseRepository.class);

    private static final ImmutableList<String> FIELDS_FOR_LIMITED_QUERY = ImmutableList.of(
            "ship.shipIdentity", "ship.brandCode", "ship.shipTridentCode");
    private static final String BUILT_AT_COUNTRY = "builtAtCountry";
    private static final String MANUFACTURER = "manufacturer";
    private static final String AGENT = "agent";
    private static final String OWNER = "owner";
    private static final String OPERATOR = "operator";
    private static final String CHARTERER = "charterer";
    private static final String NATIONALITY_COUNTRY = "nationalityCountry";
    private static final String BUCKET_ALIAS = "buck";
    @Autowired
    private RepositoryPagingAndSortService repositoryPagingAndSortService;
    @Autowired
    private Mapper mapper;
    @Autowired
    @Qualifier("bucketWithRetry")
    private Bucket bucket;
    @Autowired
    private ObjectMapper objectMapper;

    private CruiseVoyageItineraryCouchbaseRepository cviRepository;
    @Autowired
    public void setCviRepository(CruiseVoyageItineraryCouchbaseRepository cviRepository) {
        this.cviRepository = cviRepository;
    }

    private String loadOneJoinedQuery;

    public ShipCouchbaseRepository(List<Constraint<ShipEntity>> constraints, Class<ShipEntity> shipEntityClass, String entityType) {
        super(constraints, shipEntityClass, entityType);
    }

    @PostConstruct
    public void init() {
        try {
            loadOneJoinedQuery = Resources.toString(Resources.getResource("n1ql/ship/ship-one.n1ql"), Charsets.UTF_8)
                    .replace("$bucket", bucket.name());
        } catch (IOException e) {
            logger.fatal("Failed to load n1ql queries.", e);
            throw new IllegalStateException("Failed to load n1ql queries.", e);
        }
    }

    @Override
    public List<ShipEntity> findAll(Sort sort, SearchCriteriaList criterias) {
        List<ShipDtoForLimitedQuery> list = repositoryPagingAndSortService.findAll(
                FIELDS_FOR_LIMITED_QUERY,
                sort,
                ShipDtoForLimitedQuery.class,
                getPrefix() + ShipEntity.SHIP,
                criterias);
        return mapToShipEntityList(list);
    }

    @Override
    public Page<ShipEntity> findAll(Pageable pageable, SearchCriteriaList criterias) {
        Page<ShipDtoForLimitedQuery> page = repositoryPagingAndSortService.findAllPaged(
                FIELDS_FOR_LIMITED_QUERY,
                pageable,
                ShipDtoForLimitedQuery.class,
                getPrefix() + ShipEntity.SHIP,
                criterias);
        return new PageImpl<>(
                mapToShipEntityList(page.getContent()),
                pageable,
                page.getTotalElements());
    }

    public Page<ShipEntity> findAllFull(Pageable pageable, SearchCriteriaList criterias) {
        return repositoryPagingAndSortService.findAllPaged(
                ImmutableList.of("*"),
                pageable,
                ShipEntity.class,
                getPrefix() + ShipEntity.SHIP,
                criterias);
    }

    public ShipDto findOneFull(String id) {
        N1qlQueryResult rows = bucket.query(N1qlQuery.parameterized(
                loadOneJoinedQuery,
                JsonObject.create()
                        .put("id", id),
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)));

        if (!rows.errors().isEmpty()) {
            throw new PersistenceException(
                    "Errors during delete all operation "
                            + rows.errors().stream().map(JsonObject::toString).collect(Collectors.joining("; \n")),
                    ShipEntity.SHIP);
        }

        if (!rows.rows().hasNext()) {
            throw new NotFoundException(String.format("Ship %s not found", id));
        }

        return mapRowToDto(id, rows.rows().next());
    }

    public ShipDto mapRowToDto(String id, N1qlQueryRow row) {
        //N1qlQueryRow row = rows.rows().next();
        byte[] jsonBytes = row.byteValue();
        JsonObject jsonObject = row.value();
        try {
            return mapJsonToDto(jsonBytes, jsonObject);
        } catch (IOException e) {
            throw new PersistenceException("Failed to read data returned by n1ql query for entity " + id, "dive_ship", e);
        }
    }

    public ShipDto mapJsonToDto(byte[] byteValue, JsonObject jsonObject) throws IOException {
        ShipDto shipDto = objectMapper.readValue(byteValue, ShipDto.class);
        if (jsonObject.get(BUILT_AT_COUNTRY) != null)
            shipDto.getShip().getShipTechnicalSpec().getShipBuildInfo().setBuiltAtCountry(
                    objectMapper.readValue(jsonObject.get(BUILT_AT_COUNTRY).toString(), CountryDto.class));
        if (jsonObject.get(MANUFACTURER) != null)
            shipDto.getShip().getShipTechnicalSpec().getShipBuildInfo().setManufacturer(
                    objectMapper.readValue(jsonObject.get(MANUFACTURER).toString(), CompanyDto.class));
        if (jsonObject.get(AGENT) != null)
            shipDto.getShip().getShipLegal().setAgent(
                    objectMapper.readValue(jsonObject.get(AGENT).toString(), CompanyDto.class));
        if (jsonObject.get(OWNER) != null)
            shipDto.getShip().getShipLegal().setOwner(
                    objectMapper.readValue(jsonObject.get(OWNER).toString(), CompanyDto.class));
        if (jsonObject.get(OPERATOR) != null)
            shipDto.getShip().getShipLegal().setOperator(
                    objectMapper.readValue(jsonObject.get(OPERATOR).toString(), CompanyDto.class));
        if (jsonObject.get(CHARTERER) != null)
            shipDto.getShip().getShipLegal().setCharterer(
                    objectMapper.readValue(jsonObject.get(CHARTERER).toString(), CompanyDto.class));
        if (jsonObject.get(NATIONALITY_COUNTRY) != null)
            shipDto.getShip().getShipLegal().setNationalityCountry(
                    objectMapper.readValue(jsonObject.get(NATIONALITY_COUNTRY).toString(), CountryDto.class));
        shipDto.set__etag(jsonObject.getLong("_CAS").toString());
        shipDto.setId(jsonObject.getString("_ID"));
        return shipDto;
    }

    private List<ShipEntity> mapToShipEntityList(List<ShipDtoForLimitedQuery> list) {
        return list.stream()
                .map(shipDtoForLimitedQuery -> mapper.map(shipDtoForLimitedQuery, ShipEntity.class))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id, String userId) {
        Map<String, List<String>> relatedObjects = new HashMap<>();
        List<CruiseVoyageItinerary> cviList = cviRepository.findCviRelatedToShip(id);
        if(cviList.isEmpty()) {
            super.delete(id, userId);
        } else {
            ShipDto ship = findOneFull(id);
            List<String > cviIdList = cviList.stream()
                    .map(CruiseVoyageItinerary::getId)
                    .collect(Collectors.toList());
            List<String > cviNumbersList = cviList.stream()
                    .map(CruiseVoyageItinerary::getVoyageNumber)
                    .collect(Collectors.toList());
            relatedObjects.put(CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY, cviIdList);

            String message = "Can't delete ship with name " + ship.getShip().getShipIdentity().getName() +
                    " (official number: " + ship.getShip().getShipIdentity().getOfficialNumber() + ") " +
                    "because it has related Cruise Voyage Itineraries: " +
                    cviNumbersList.stream().collect(Collectors.joining(", ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }

    public List<ShipDto> findShipsRelatedToCompanies(List<String> companyIds){
        String bucketName = bucket.name();

        String query = String.format("SELECT * " +
                "FROM `%s` AS %s " +
                "WHERE `__type` = $type " +
                "AND ( " +
                "%s.ship.shipLegal.agentId IN $companyIds OR " +
                "%s.ship.shipLegal.chartererId IN $companyIds OR " +
                "%s.ship.shipLegal.operatorId IN $companyIds OR " +
                "%s.ship.shipLegal.ownerId IN $companyIds OR " +
                "%s.ship.shipTechnicalSpec.shipBuildInfo.manufacturerId IN $companyIds " +
                ")", bucketName, BUCKET_ALIAS, BUCKET_ALIAS, BUCKET_ALIAS, BUCKET_ALIAS, BUCKET_ALIAS, BUCKET_ALIAS);

        JsonObject parameters = JsonObject.create();
        parameters.put("companyIds", JsonArray.from(companyIds));
        parameters.put("bucket", bucketName);
        parameters.put("type", getPrefix() + ShipEntity.SHIP);

        logger.info("Executing N1QL:\n " + query
                + " \n\nParameters: \n" + parameters);


        N1qlQueryResult rows = bucket.query(N1qlQuery.parameterized(
                query,
                parameters,
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)));

        if (!rows.errors().isEmpty()) {
            throw new PersistenceException(
                    "Errors during select "
                            + rows.errors().stream().map(JsonObject::toString).collect(Collectors.joining("; \n")),
                    ShipEntity.SHIP);
        }

        return rows.allRows().stream()
                .map(n1qlQueryRow -> mapRowToDto(n1qlQueryRow))
                .collect(Collectors.toList());
    }

    private ShipDto mapRowToDto(N1qlQueryRow row) {
        ShipDto shipDto = null;
        try {
            shipDto = objectMapper.readValue(row.value().get(BUCKET_ALIAS).toString(), ShipDto.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shipDto;
    }
}
