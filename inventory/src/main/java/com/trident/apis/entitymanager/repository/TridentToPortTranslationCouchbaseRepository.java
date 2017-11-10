package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.apis.entitymanager.model.TridentToPortTranslation;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vdmitrovskiy on 7/31/17.
 */
public class TridentToPortTranslationCouchbaseRepository extends RepositoryDecorator<TridentToPortTranslation>{
    @Autowired
    RepositoryDecorator<ApisRule> apisRuleRepositoryDecorator;

    public TridentToPortTranslationCouchbaseRepository(List<Constraint<TridentToPortTranslation>> constraints, Class<TridentToPortTranslation> tridentToPortTranslationClass, String entityType) {
        super(constraints, tridentToPortTranslationClass, entityType);
    }

    @Override
    public void delete(String id, String userId) {
        Map<String, List<String>> relatedObjects = new HashMap<>();
        List<ApisRule> relatedApisRules = apisRuleRepositoryDecorator.findAll(
                new SearchCriteriaList(new SearchEqualCriteria<>( "tridentToPortTranslationId", id)));

        if (relatedApisRules.isEmpty()){
            super.delete(id, userId);
        } else {
            TridentToPortTranslation tridentToPortTranslation = findOne(id);
            List<String> apisRuleIds = relatedApisRules.stream()
                    .map(ApisRule::getId)
                    .collect(Collectors.toList());
            List<String> apisRuleNames = relatedApisRules.stream()
                    .map(ApisRule::getName)
                    .collect(Collectors.toList());
            relatedObjects.put(ApisRule.APIS_RULE, apisRuleIds);

            String message = "Can't delete Trident To PortDto Translation " + tridentToPortTranslation.getName() +
                    " (id: " + tridentToPortTranslation.getId() + ") " +
                    "because it has related Apis Rules: " +
                    apisRuleNames.stream().collect(Collectors.joining(", ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }
}
