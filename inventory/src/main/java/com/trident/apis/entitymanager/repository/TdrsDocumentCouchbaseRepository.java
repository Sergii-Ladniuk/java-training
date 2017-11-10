package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.apis.entitymanager.model.CodeDictionary;
import com.trident.apis.entitymanager.model.TridentToPortTranslation;
import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.shared.immigration.dto.tdrs.TdrsAttributeDto;
import com.trident.shared.immigration.dto.tdrs.TdrsAttributeSubType;
import com.trident.shared.immigration.dto.tdrs.TdrsAttributeType;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vdmitrovskiy on 7/31/17.
 */
public class TdrsDocumentCouchbaseRepository extends RepositoryDecorator<TdrsDocument> {

    private static final Logger logger = Logger.getLogger(TdrsDocumentCouchbaseRepository.class);

    @Autowired
    private TdrsRuleCouchbaseRepository tdrsRuleCouchbaseRepository;
    @Autowired
    private RepositoryDecorator<CodeDictionary> codeDictionaryRepositoryDecorator;

    public TdrsDocumentCouchbaseRepository(List<Constraint<TdrsDocument>> constraints, Class<TdrsDocument> tdrsDocumentClass, String entityType) {
        super(constraints, tdrsDocumentClass, entityType);
    }

    @Override
    public void delete(String id, String userId) {
        Map<String, List<String>> relatedObjects = new HashMap<>();
        List<TdrsRule> relatedTdrsRules = tdrsRuleCouchbaseRepository.findAll(
                new SearchCriteriaList(new SearchInArrayFieldCriteria<>("documentIds", id)));

        if (relatedTdrsRules.isEmpty()) {
            super.delete(id, userId);
        } else {
            TdrsDocument tdrsDocument = findOne(id);
            List<String> apisRuleIds = relatedTdrsRules.stream()
                    .map(TdrsRule::getId)
                    .collect(Collectors.toList());
            List<String> apisRuleNames = relatedTdrsRules.stream()
                    .map(TdrsRule::getName)
                    .collect(Collectors.toList());
            relatedObjects.put(TdrsRule.TDRS_RULE, apisRuleIds);

            String message = "Can't delete Tdrs Document " + tdrsDocument.getName() +
                    " (id: " + tdrsDocument.getId() + ") " +
                    "because it has related Tdrs Rule: " +
                    apisRuleNames.stream().collect(Collectors.joining(", ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }

    @PostConstruct
    public void init() {
        try {
            List<TdrsDocument> tdrsDocuments = findAll();
            CodeDictionary attributeTypes = codeDictionaryRepositoryDecorator.findOne("dive-tdrs-attributes-types");
            tdrsDocuments.forEach(tdrsDocument -> {
                Set<TdrsAttributeDto> existingAttributes = Arrays.stream(tdrsDocument.getTdrsAttributes()).collect(Collectors.toSet());
                List<TdrsAttributeDto> toBeAdded = attributeTypes.getCodes().stream()
                        .filter(code -> existingAttributes.stream().noneMatch(attr -> code.equals(attr.getApisFieldType())))
                        .filter(code -> !code.equals("N/A"))
                        .map(s -> TdrsAttributeDto.builder()
                                .apisFieldType(s)
                                .type(s.contains("_dt") ? TdrsAttributeType.DATE : TdrsAttributeType.STRING)
                                .subType(s.contains("expiration") ? TdrsAttributeSubType.EXPIRATION_DATE : TdrsAttributeSubType.OTHER)
                                .name(s.replaceAll("_", " ").toUpperCase())
                                .build())
                        .collect(Collectors.toList());
                if (toBeAdded.size() > 0) {
                    tdrsDocument.setTdrsAttributes(
                            Stream.concat(existingAttributes.stream(), toBeAdded.stream()).toArray(TdrsAttributeDto[]::new)
                    );
                    update(tdrsDocument, null);
                    logger.info("Updated " + tdrsDocument);
                }
                if (StringUtils.isEmpty(tdrsDocument.getCode())) {
                    tdrsDocument.setCode(tdrsDocument.getName().toLowerCase()
                            .replaceAll("\\s+", "_")
                            .replaceAll("\\W+", ""));
                    update(tdrsDocument, null);
                }

            });
        } catch (Throwable e) {
            logger.error("Error in TdrsDocumentCouchbaseRepository initialization.", e);
        }
    }
}
