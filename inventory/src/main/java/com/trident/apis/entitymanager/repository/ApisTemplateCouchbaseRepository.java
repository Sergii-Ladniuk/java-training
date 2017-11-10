package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.*;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vdmitrovskiy on 7/27/17.
 */
public class ApisTemplateCouchbaseRepository extends RepositoryDecorator<ApisTemplate> {

    @Lazy
    @Autowired
    private RepositoryDecorator<ApisRule> apisRuleRepositoryDecorator;
    @Lazy
    @Autowired
    private RepositoryDecorator<DocumentContent> documentContentCouchbaseRepository;

    public ApisTemplateCouchbaseRepository(List<Constraint<ApisTemplate>> constraints, Class<ApisTemplate> apisTemplateClass, String entityType) {
        super(constraints, apisTemplateClass, entityType);
    }

    @Override
    public ApisTemplate findOne(String id) {
        ApisTemplate template = super.findOne(id);
        if(template.getType() == DocumentOutputType.PLAIN){
            if(template.getNoteId() != null) {
                DocumentContent content = documentContentCouchbaseRepository.findOne(template.getNoteId());
                template.setNotes(content.getContent());
            }

            if(template.getContentId() != null){
                DocumentContent content = documentContentCouchbaseRepository.findOne(template.getContentId());
                template.setContent(content.getContent());
            }
        }

        return template;
    }

    @Override
    public void delete(String id, String userId) {

        Map<String, List<String>> relatedObjects = new HashMap<>();
        List<ApisRule> relatedApisRules = apisRuleRepositoryDecorator.findAll(
                new SearchCriteriaList(new SearchInArrayFieldCriteria<>( "apisTemplateIds", id)));

        if (relatedApisRules.isEmpty()){
            super.delete(id, userId);
        } else {
            ApisTemplate apisTemplate = findOne(id);
            List<String> apisRuleIds = relatedApisRules.stream()
                    .map(ApisRule::getId)
                    .collect(Collectors.toList());
            List<String> apisRuleNames = relatedApisRules.stream()
                    .map(ApisRule::getName)
                    .collect(Collectors.toList());
            relatedObjects.put(ApisRule.APIS_RULE, apisRuleIds);

            String message = "Can't delete Apis Template " + apisTemplate.getName() +
                    "because it has related Apis Rules: " +
                    apisRuleNames.stream().collect(Collectors.joining(", ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }
}
