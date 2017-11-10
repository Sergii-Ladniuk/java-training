package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.apis.entitymanager.service.tdrs.TdrsRecommendationTreeMapper;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by vdmitrovskiy on 7/31/17.
 */
public class TdrsRuleCouchbaseRepository extends RepositoryDecorator<TdrsRule> {
    @Autowired
    TdrsRecommendationTreeMapper tdrsRecommendationTreeMapper;

    public TdrsRuleCouchbaseRepository(List<Constraint<TdrsRule>> constraints, Class<TdrsRule> tdrsRuleClass, String entityType) {
        super(constraints, tdrsRuleClass, entityType);
    }

    @Override
    protected TdrsRule doSave(TdrsRule tdrsRule) {
        tdrsRule.setDocumentIds(getAllDocumentIds(tdrsRule));
        return super.doSave(tdrsRule);
    }

    @Override
    protected TdrsRule doSave(TdrsRule tdrsRule, WriteMode writeMode) {
        tdrsRule.setDocumentIds(getAllDocumentIds(tdrsRule));
        return super.doSave(tdrsRule, writeMode);
    }

    private Set<String> getAllDocumentIds(TdrsRule tdrsRule){
        Set<String> idsSet = new HashSet<>();
        idsSet.addAll(tdrsRecommendationTreeMapper.getDocumentIds(tdrsRule.getRecommendation()));
        return idsSet;
    }
}
