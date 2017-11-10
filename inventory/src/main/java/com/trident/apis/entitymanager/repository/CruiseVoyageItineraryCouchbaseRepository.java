package com.trident.apis.entitymanager.repository;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.ParameterizedN1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.shared.immigration.correlationid.CorrelationIdService;
import com.trident.shared.immigration.dto.apis.ApisSubmission;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.dto.apis.cvi.CviPortEntry;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.dto.apis.port.PortDto;
import com.trident.shared.immigration.exception.CviAlreadyExistsException;
import com.trident.shared.immigration.exception.PersistenceException;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.ArrayContainsObjectWithFieldSpecifiedCriteria;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.util.DatetimeUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CruiseVoyageItineraryCouchbaseRepository extends RepositoryDecorator<CruiseVoyageItinerary> {

    private static final Logger logger = Logger.getLogger(CruiseVoyageItineraryCouchbaseRepository.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CorrelationIdService correlationIdService;

    public CruiseVoyageItineraryCouchbaseRepository(List<Constraint<CruiseVoyageItinerary>> constraints, Class<CruiseVoyageItinerary> cruiseVoyageItineraryClass, String entityType) {
        super(constraints, cruiseVoyageItineraryClass, entityType);
    }

    public CruiseVoyageItineraryDto findOneFull(String id) {
        CruiseVoyageItinerary cvi = findOne(id);
        CruiseVoyageItineraryDto cviDto = createCviDtoFromEntity(cvi);
        return cviDto;
    }

    private CruiseVoyageItineraryDto createCviDtoFromEntity(CruiseVoyageItinerary cvi) {
        final List<String> errorsList = Collections.synchronizedList(new ArrayList<>());

        CruiseVoyageItineraryDto cviDto = new CruiseVoyageItineraryDto();
        cviDto.setId(cvi.getId());
        cviDto.set__etag(cvi.get__etag());
        cviDto.set__type(cvi.get__type());
        cviDto.setBrandCode(cvi.getBrandCode());
        cviDto.setVoyageNumber(cvi.getVoyageNumber());
        cviDto.setDescription(cvi.getDescription());
        cviDto.setNotes(cvi.getNotes());
        cviDto.setStartDate(cvi.getStartDate());
        cviDto.setEndDate(cvi.getEndDate());
        cviDto.setShipCode(cvi.getShipCode());
        cviDto.setShipId(cvi.getShipId());
        cviDto.setDateModified(cvi.getDateModified());
        cviDto.setCviPortEntries(new ArrayList<>(cvi.getCviPortEntries().size()));

        CountDownLatch countDownLatch = new CountDownLatch(getAsyncOperationCount(cvi));

        cvi.getCviPortEntries().stream()
                .forEach((CviPortEntry cviPortEntry) -> {
                    CviPortEntry cviPortEntryDto = createCviPortEntryDtoFromEntity(cviPortEntry, countDownLatch, errorsList);

                    synchronized (cviDto) {
                        cviDto.getCviPortEntries().add(cviPortEntryDto);
                    }
                });

        fillShipToCviDto(cviDto, cvi.getShipId(), countDownLatch, errorsList);

        try {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            errorsList.add(e.getMessage());
            logger.warn("Interrupted", e);
        }

        if (!errorsList.isEmpty()) {
            throw new PersistenceException(String.join("\n", errorsList), "Cruise Voyage Itinerary");
        }

        return cviDto;
    }

    private CviPortEntry createCviPortEntryDtoFromEntity(CviPortEntry cviPortEntry, CountDownLatch countDownLatch, List<String> errorsList) {
        CviPortEntry cviPortEntryDto = new CviPortEntry();
        cviPortEntryDto.setId(cviPortEntry.getId());
        cviPortEntryDto.setArrivalDate(cviPortEntry.getArrivalDate());
        cviPortEntryDto.setDepartureDate(cviPortEntry.getDepartureDate());
        cviPortEntryDto.setVoyageTypeArrival(cviPortEntry.getVoyageTypeArrival());
        cviPortEntryDto.setVoyageTypeDeparture(cviPortEntry.getVoyageTypeDeparture());
        cviPortEntryDto.setPortId(cviPortEntry.getPortId());
        cviPortEntryDto.setApisSubmissionIds(cviPortEntry.getApisSubmissionIds());

        if (cviPortEntry.getApisSubmissionIds() != null) {
            cviPortEntryDto.setApisSubmissions(new ArrayList<>(cviPortEntry.getApisSubmissionIds().size()));
            fillCviPortEntryDtoApisSubmission(cviPortEntryDto, cviPortEntry.getApisSubmissionIds(), countDownLatch, errorsList);
        }

        fillCviPortEntryDtoPort(cviPortEntryDto, cviPortEntry.getPortId(), countDownLatch, errorsList);

        return cviPortEntryDto;
    }

    private void fillCviPortEntryDtoApisSubmission(CviPortEntry cviPortEntry,
                                                   List<String> apisSubmissionIds,
                                                   CountDownLatch countDownLatch,
                                                   List<String> errorsList) {
        logger.info("Loading submissions for CVI by ids: " + apisSubmissionIds.toString());
        getBucket().async().query(getApisSubmissionsByIdsQuery(apisSubmissionIds))
                .subscribe(asyncN1qlQueryResult -> {
                            asyncN1qlQueryResult.errors().subscribe(errors -> {
                                        if (!errors.isEmpty()) {
                                            errorsList.add(errors.toString());
                                            logger.error("Error while fetching submissions: " + errors.toString());
                                            countDownLatch.countDown();
                                        }
                                    },
                                    error -> {
                                        logger.error(error.getMessage(), error);
                                        errorsList.add(error.getMessage());
                                    });

                            asyncN1qlQueryResult.rows()
                                    .subscribe(asyncN1qlQueryRow -> {
                                                try {
                                                    ApisSubmission apisSubmission = objectMapper.readValue(
                                                            asyncN1qlQueryRow.byteValue(),
                                                            ApisSubmission.class);
                                                    synchronized (cviPortEntry) {
                                                        cviPortEntry.getApisSubmissions().add(apisSubmission);
                                                    }
                                                    countDownLatch.countDown();
                                                } catch (Throwable e) {
                                                    logger.error(e.getMessage(), e);
                                                    errorsList.add(e.getMessage());
                                                }
                                            },
                                            error -> {
                                                logger.error(error.getMessage(), error);
                                                errorsList.add(error.getMessage());
                                            });
                        },
                        error -> {
                            logger.error(error.getMessage(), error);
                            errorsList.add(error.getMessage());
                        });
    }

    private void fillCviPortEntryDtoPort(CviPortEntry cviPortEntry, String portId, CountDownLatch countDownLatch, List<String> errorsList) {
        getBucket().async().get(portId).subscribe(jsonDocument -> {
                    try {
                        synchronized (cviPortEntry) {
                            cviPortEntry.setPort(objectMapper.readValue(jsonDocument.content().toString(), PortDto.class));
                        }
                        countDownLatch.countDown();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                        errorsList.add(e.getMessage());
                    }
                },
                error -> {
                    logger.error(error.getMessage(), error);
                    errorsList.add(error.getMessage());
                });
    }

    private void fillShipToCviDto(CruiseVoyageItineraryDto cviDto, String shipId, CountDownLatch countDownLatch, List<String> errorsList) {
        getBucket().async().get(shipId).subscribe(jsonDocument -> {
                    try {

                        ShipDto ship = objectMapper.readValue(jsonDocument.content().toString(), ShipDto.class);
                        synchronized (cviDto) {
                            cviDto.setShip(ship);
                        }
                        countDownLatch.countDown();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                        errorsList.add(e.getMessage());
                    }
                },
                error -> {
                    logger.error(error.getMessage(), error);
                    errorsList.add(error.getMessage());
                });
    }

    private int getAsyncOperationCount(CruiseVoyageItinerary cvi) {
        return cvi.getCviPortEntries().size() + submissionCount(cvi) + 1;
    }

    private int submissionCount(CruiseVoyageItinerary cvi) {
        return (int) cvi.getCviPortEntries().stream()
                .filter(cviPortEntry -> cviPortEntry.getApisSubmissionIds() != null)
                .flatMap(cviPortEntry -> cviPortEntry.getApisSubmissionIds().stream())
                .count();
    }

    private ParameterizedN1qlQuery getApisSubmissionsByIdsQuery(List<String> apisSubmissionIds) {
        return N1qlQuery.parameterized(
                "SELECT meta(`apisSub`).id, meta(`apisSub`).CAS, `apisSub`.`ruleId`, `apisSub`.`ruleName`, `apisSub`.`portId`, `apisSub`.`portName`, `apisSub`.`startDate`, `apisSub`.`statusDate`, `apisSub`.`errors`, `apisSub`.`status`, `apisSub`.`submissionMethod`, `apisSub`.`trackingReference`,`apisSub`.`direction`,`apisSub`.`ruleType` " +
                        "FROM `" + getBucket().name() + "` as apisSub " +
                        "WHERE __type='dive_apisSubmission' and meta(`apisSub`).id IN $1", JsonArray.from(JsonArray.from(apisSubmissionIds)));
    }

    public List<CruiseVoyageItinerary> findCviRelatedToShip(String shipId) {
        return findAll(new SearchCriteriaList(new SearchEqualCriteria("shipId", shipId)));
    }

    public List<CruiseVoyageItinerary> findCviRelatedToPort(String portId) {
        return findAll(new SearchCriteriaList(new ArrayContainsObjectWithFieldSpecifiedCriteria<String>("cviPortEntries", "portId", portId)));

    }

    @Override
    public CruiseVoyageItinerary save(CruiseVoyageItinerary entity, String userId, WriteMode writeMode, boolean bulkModeDisabled) {
        entity.setDateModified(DatetimeUtil.currentTimeInMsUTC());
        if (!bulkModeDisabled) {
            return super.save(entity, userId, writeMode, bulkModeDisabled);
        } else {
            JsonDocument jsonDocument = lock("CVI_SAVE_LOCK");
            try {
                verifyNoCviForSameShipAndDates(entity, entity.getShipId(), entity.getStartDate(), entity.getEndDate());
                return super.save(entity, userId, writeMode, bulkModeDisabled);
            } finally {
                logger.debug("Releasing " + jsonDocument.id());
                getBucket().unlock(jsonDocument);
            }
        }
    }

    private void verifyNoCviForSameShipAndDates(CruiseVoyageItinerary entity, String shipId, long startDate, long endDate) {
        List<CruiseVoyageItineraryDto> matchedCvis = findAllByShipAndDates(shipId, startDate, endDate);
        if (matchedCvis.stream().filter(cvi -> !cvi.getId().equals(entity.getId())).count() > 0) {
            throw new CviAlreadyExistsException(matchedCvis);
        }
    }

    public List<CruiseVoyageItineraryDto> findAllByShipAndDates(String shipId, long startDate, long endDate) {
        ParameterizedN1qlQuery query = getQueryCviForSameShipAndDates(shipId, startDate, endDate);
        N1qlQueryResult queryResult = getBucket().query(query);
        verifyNoN1qlErrors(query, queryResult);
        return parseCvi(queryResult);
    }


    private List<CruiseVoyageItineraryDto> parseCvi(N1qlQueryResult queryResult) {
        return queryResult.allRows().stream()
                .map(n1qlQueryRow -> {
                    try {
                        return objectMapper.readValue(n1qlQueryRow.byteValue(), CruiseVoyageItineraryDto.class);
                    } catch (IOException e) {
                        throw new PersistenceException("Failed to parse CVI.", CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY, e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void verifyNoN1qlErrors(ParameterizedN1qlQuery query, N1qlQueryResult queryResult) {
        if (queryResult.errors().size() > 0) {
            throw new PersistenceException(
                    "Errors during executing N1QL query " +
                            query.statement().toString() +
                            queryResult.errors().stream()
                                    .map(JsonObject::toString)
                                    .collect(Collectors.joining("\n", "\n", "\n")) +
                            " parameters: " + query.statementParameters(),
                    CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY);
        }
    }

    private ParameterizedN1qlQuery getQueryCviForSameShipAndDates(String shipId, long startDate, long endDate) {
        return N1qlQuery.parameterized(
                "SELECT meta(`b`).id, startDate, endDate " +
                        " FROM `" + getBucket().name() + "` as b" +
                        " WHERE __type=$type AND shipId = $shipId AND (" +
                        "(startDate <= $startDate AND endDate >= $endDate) OR " +
                        "(startDate >= $startDate AND startDate <= $endDate) OR " +
                        "(endDate >= $startDate AND endDate <= $endDate)" +
                        ")",
                JsonObject.create()
                        .put("shipId", shipId)
                        .put("startDate", startDate)
                        .put("endDate", endDate)
                        .put("type", getPrefix() + CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY),
                N1qlParams.build().consistency(ScanConsistency.STATEMENT_PLUS)
        );
    }
}
