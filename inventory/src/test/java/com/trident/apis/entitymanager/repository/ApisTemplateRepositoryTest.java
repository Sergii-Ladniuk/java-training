package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by dimzul on 12/6/16.
 */
public class ApisTemplateRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RepositoryDecorator<ApisTemplate> apisTemplateRepository;

    @Override
    protected MockDataProvider.MockType getType() {
        return MockDataProvider.MockType.APIS_TEMPLATE;
    }

    @Override
    protected int getMockCount() {
        return 5;
    }

    @Override
    protected RepositoryDecorator<ApisTemplate> getRepository() {
        return apisTemplateRepository;
    }

    @Override
    protected String getModelCouchbaseName() {
        return RepositoryDecorator.getPrefix() + "apisTemplate";
    }

    @Override
    protected Class getModelClass() {
        return ApisTemplate.class;
    }
}
