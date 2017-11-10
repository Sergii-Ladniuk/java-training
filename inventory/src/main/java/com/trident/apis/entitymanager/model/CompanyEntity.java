package com.trident.apis.entitymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.dto.apis.knowledge.legal.Company;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;

public class CompanyEntity extends CouchbaseEntityWithId {

    public static final String COMPANY = "company";

    private Company company;

    public CompanyEntity() {
        super(COMPANY);
    }

    public static CompanyEntity of(String id, Company company) {
        CompanyEntity companyEntity  = new CompanyEntity();
        companyEntity.setId(id);
        companyEntity.setCompany(company);
        return companyEntity;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return company.getName();
    }
}
