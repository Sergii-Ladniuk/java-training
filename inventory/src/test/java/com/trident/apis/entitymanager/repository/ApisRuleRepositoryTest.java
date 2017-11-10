package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by dimzul on 12/6/16.
 */
public class ApisRuleRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RepositoryDecorator<ApisRule> repository;

    @Override
    protected MockDataProvider.MockType getType() {
        return MockDataProvider.MockType.APIS_RULE;
    }

    @Override
    protected int getMockCount() {
        return 5;
    }

    @Override
    protected RepositoryDecorator<ApisRule> getRepository() {
        return repository;
    }

    @Override
    protected String getModelCouchbaseName() {
        return RepositoryDecorator.getPrefix() + "apisRule";
    }

    @Override
    protected Class getModelClass() {
        return ApisRule.class;
    }
}
