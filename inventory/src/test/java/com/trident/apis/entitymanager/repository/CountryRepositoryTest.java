package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.Country;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.is;

/**
 * Created by Sergii Ladniuk on 11/16/16.
 */
public class CountryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RepositoryDecorator<Country> countryRepository;

    @Override
    protected MockDataProvider.MockType getType() {
        return MockDataProvider.MockType.COUNTRY;
    }

    @Override
    protected int getMockCount() {
        return 5;
    }

    @Override
    protected RepositoryDecorator<Country> getRepository() {
        return countryRepository;
    }

    @Override
    protected String getModelCouchbaseName() {
        return RepositoryDecorator.getPrefix() + "country";
    }

    @Override
    protected Class getModelClass() {
        return Country.class;
    }
}