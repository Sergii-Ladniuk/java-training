package com.trident.apis.entitymanager.repository;

import com.couchbase.client.java.Bucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ApisSubmission;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.exception.PersistenceException;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vdmitrovskiy on 7/27/17.
 */
public class ApisSubmissionCouchbaseRepository extends RepositoryDecorator<ApisSubmission>{

    public static final String BUCKET_ALIAS = "buck";
    public static final String DIVE_ = "dive_";

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

    private static final Logger logger = Logger.getLogger(ShipCouchbaseRepository.class);

    public ApisSubmissionCouchbaseRepository(List<Constraint<ApisSubmission>> constraints, Class<ApisSubmission> apisSubmissionClass, String entityType) {
        super(constraints, apisSubmissionClass, entityType);
    }

    public List<ApisSubmission> findApisSubmissionsRelatedToPort(String id){
        return findAll(new SearchCriteriaList(new SearchEqualCriteria("portId", id)));
    }

    @Override
    public void delete(String id, String userId) {
        //TODO CviEntry
        super.delete(id, userId);
    }

    @Override
    public ApisSubmission findOne(String id) {
        ApisSubmission submission = super.findOne(id);
        if (submission == null) {
            throw new PersistenceException("Database return null value", "ApisSubmission");
        }
        return submission;
    }
}
