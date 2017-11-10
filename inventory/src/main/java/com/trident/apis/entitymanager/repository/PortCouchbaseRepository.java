package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.*;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.shared.immigration.api.client.util.DateTimeFormatService;
import com.trident.shared.immigration.exception.PersistenceException;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.ArrayContainsObjectWithFieldSpecifiedCriteria;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import com.trident.shared.immigration.util.DatetimeUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by vdmitrovskiy on 7/26/17.
 */

public class PortCouchbaseRepository extends RepositoryDecorator<Port> {

    @Autowired
    ApisSubmissionCouchbaseRepository apisSubmissionCouchbaseRepository;
    @Autowired
    CruiseVoyageItineraryCouchbaseRepository cruiseVoyageItineraryCouchbaseRepository;
    @Autowired
    RepositoryDecorator<ApisRule> apisRuleRepositoryDecorator;
    @Autowired
    DateTimeFormatService dateTimeFormatService;

    public PortCouchbaseRepository(List<Constraint<Port>> constraints, Class<Port> portClass, String entityType) {
        super(constraints, portClass, entityType);
    }

    @Override
    public Port findOne(String id) {
        Port port = super.findOne(id);
        if (port == null) {
            throw new PersistenceException("Database return null value", "Port");
        }
        return port;
    }

    @Override
    public void delete(String id, String userId) {

        apisSubmissionCouchbaseRepository.findApisSubmissionsRelatedToPort(id);

        Map<String, List<String>> relatedObjects = new HashMap<>();

        List<ApisSubmission> relatedApisSubmissionsToPort = apisSubmissionCouchbaseRepository.findApisSubmissionsRelatedToPort(id);
        List<CruiseVoyageItinerary> relatedCviToPort = cruiseVoyageItineraryCouchbaseRepository.findCviRelatedToPort(id);
        List<ApisRule> relatedApisRuleToPort = apisRuleRepositoryDecorator.findAll(
                new SearchCriteriaList(new SearchInArrayFieldCriteria<>("apisPortIds", id)));

        if (relatedApisSubmissionsToPort.isEmpty() && relatedCviToPort.isEmpty()
                && relatedApisRuleToPort.isEmpty()){
            super.delete(id, userId);
        } else {
            Port port = findOne(id);

            List<String> messageParts = new ArrayList<>();
            int i = 1;

            if (!relatedApisSubmissionsToPort.isEmpty()){
                List<String> apisSubmissionIds = relatedApisSubmissionsToPort.stream()
                        .map(ApisSubmission::getId)
                        .collect(Collectors.toList());
                List<String> apisSubmissionDescriptions = relatedApisSubmissionsToPort.stream()
                        .map(apisSubmission -> "PortDto: "+ apisSubmission.getPortName() +
                                "Rule: " + apisSubmission.getRuleName() +
                                "Status Date: "+ dateTimeFormatService.timestampInUserFriendlyString(apisSubmission.getStatusDate(),
                                                                                                        TimeZone.getTimeZone("GMT")))
                        .collect(Collectors.toList());
                relatedObjects.put(ApisSubmission.APIS_SUBMISSION, apisSubmissionIds);

                String message = i + ") it has related Apis Submissions: "
                        + apisSubmissionDescriptions.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedCviToPort.isEmpty()){
                List<String> cviIds = relatedCviToPort.stream()
                        .map(CruiseVoyageItinerary::getId)
                        .collect(Collectors.toList());
                relatedObjects.put(CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY, cviIds);

                String message = i + ") it has related Cruise Voyage Itinerary by CVI Entry with ids: "
                        + cviIds.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedApisRuleToPort.isEmpty()){
                List<String> apisRuleIds = relatedApisRuleToPort.stream()
                        .map(ApisRule::getId)
                        .collect(Collectors.toList());
                relatedObjects.put(ApisRule.APIS_RULE, apisRuleIds);

                String message = i + ") it has related Apis Rules with ids: "
                        + apisRuleIds.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            String message = "Can't delete PortDto " + port.getName() +
                    " (polar code: " + port.getPolarCode() + ", UN code: "+port.getUnCode()+") because: " +
                    messageParts.stream().collect(Collectors.joining("; ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }

    }
}
