package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.Port;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Sergii Ladniuk on 11/16/16.
 */
public class PortRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RepositoryDecorator<Port> portRepository;

    @Override
    protected MockDataProvider.MockType getType() {
        return MockDataProvider.MockType.PORT;
    }

    @Override
    protected int getMockCount() {
        return 5;
    }

    @Override
    protected RepositoryDecorator<Port> getRepository() {
        return portRepository;
    }

    @Override
    protected String getModelCouchbaseName() {
        return RepositoryDecorator.getPrefix() + "port";
    }

    @Override
    protected Class getModelClass() {
        return Port.class;
    }
}