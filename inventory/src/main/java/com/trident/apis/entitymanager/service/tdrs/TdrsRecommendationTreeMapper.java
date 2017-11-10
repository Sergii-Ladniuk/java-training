package com.trident.apis.entitymanager.service.tdrs;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.apis.entitymanager.repository.TdrsDocumentCouchbaseRepository;
import com.trident.apis.entitymanager.repository.TdrsRuleCouchbaseRepository;
import com.trident.shared.immigration.dto.tdrs.TdrsDocumentDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.DocumentNode;
import com.trident.shared.immigration.dto.tdrs.recommendation.DocumentNodeDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.LogicNode;
import com.trident.shared.immigration.dto.tdrs.recommendation.RecommendationNode;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchIdInCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TdrsRecommendationTreeMapper {

    @Autowired
    private TdrsDocumentCouchbaseRepository tdrsDocumentRepository;

    public RecommendationNode toFlatRecommendation(RecommendationNode root) {
        return iterate(root, node -> convertDocumentNodeDto((DocumentNodeDto) node));
    }

    public RecommendationNode toFullRecommendation(RecommendationNode root) {
        List<String> documentIds = getDocumentIds(root);
        List<TdrsDocument> documents = tdrsDocumentRepository.findAll(new SearchCriteriaList(new SearchIdInCriteria(documentIds)));
        Map<String, TdrsDocument> documentMap = documents.stream().collect(Collectors.toMap(CouchbaseEntityWithId::getId, d -> d));
        return iterate(root, recommendationNode -> {
            DocumentNode documentNode = (DocumentNode) recommendationNode;
            TdrsDocument tdrsDocument = documentMap.get(documentNode.getDocumentId());
            TdrsDocumentDto document = tdrsDocument.toDto();
            return new DocumentNodeDto(document);
        });
    }

    public List<String> getDocumentIds(RecommendationNode root) {
        if (root instanceof LogicNode) {
            LogicNode logicNode = (LogicNode) root;
            return logicNode.getList().stream()
                    .flatMap(recommendationNode -> getDocumentIds(recommendationNode).stream())
                    .collect(Collectors.toList());
        } else if (root instanceof DocumentNode) {
            DocumentNode documentNode = (DocumentNode) root;
            return ImmutableList.of(documentNode.getDocumentId());
        } else {
            throw new IllegalArgumentException("Unexpected RecommendationNode type: " + root);
        }
    }

    private RecommendationNode iterate(RecommendationNode root, Function<RecommendationNode, RecommendationNode> converter) {
        if (root instanceof LogicNode) {
            LogicNode logicNode = (LogicNode) root;
            Set<RecommendationNode> list = logicNode.getList().stream().map(recommendationNode -> iterate(recommendationNode, converter)).collect(Collectors.toSet());
            return new LogicNode(logicNode.getType(), list);
        } else if (root instanceof DocumentNodeDto || root instanceof DocumentNode) {
            return converter.apply(root);
        } else {
            throw new IllegalArgumentException("Unexpected RecommendationNode type: " + root);
        }
    }

    private RecommendationNode convertDocumentNodeDto(DocumentNodeDto documentNodeDto) {
        return new DocumentNode(documentNodeDto.getDocument().getId());
    }

}
