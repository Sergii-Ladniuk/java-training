package com.trident.apis.entitymanager.service;

import com.trident.apis.entitymanager.model.CompanyEntity;
import com.trident.shared.immigration.dto.apis.knowledge.legal.CompanyDto;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CompanyMapper {

    private RepositoryDecorator<CompanyEntity> companyRepository;
    private Mapper mapper;

    public CompanyDto idToDto(String id) {
        return mapper.map(companyRepository.findOne(id), CompanyDto.class);
    }

    @Autowired
    public void setCompanyRepository(RepositoryDecorator<CompanyEntity> companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Lazy
    @Autowired
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }
}
