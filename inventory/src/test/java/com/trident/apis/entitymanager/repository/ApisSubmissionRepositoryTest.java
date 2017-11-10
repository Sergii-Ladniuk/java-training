package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisSubmission;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;

public class ApisSubmissionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RepositoryDecorator<ApisSubmission> repository;

    @Override
    protected MockDataProvider.MockType getType() {
        return MockDataProvider.MockType.APIS_SUBMISSION;
    }

    @Override
    protected int getMockCount() {
        return 5;
    }

    @Override
    protected RepositoryDecorator<ApisSubmission> getRepository() {
        return repository;
    }

    @Override
    protected String getModelCouchbaseName() {
        return RepositoryDecorator.getPrefix() + "apisSubmission";
    }

    @Override
    protected Class getModelClass() {
        return ApisSubmission.class;
    }
}
