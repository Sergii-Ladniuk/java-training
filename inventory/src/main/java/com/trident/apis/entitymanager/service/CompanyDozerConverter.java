package com.trident.apis.entitymanager.service;

import com.trident.shared.immigration.dto.apis.knowledge.legal.CompanyDto;
import org.dozer.CustomConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyDozerConverter implements CustomConverter {

    private CompanyMapper companyMapper;

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue == null) {
            return null;
        } else if (destinationClass.equals(String.class) && sourceClass.getCanonicalName().equals(CompanyDto.class.getCanonicalName())) {
            CompanyDto companyDto = (CompanyDto) sourceFieldValue;
            return companyDto.getId();
        } else if (destinationClass.equals(CompanyDto.class) && sourceClass.equals(String.class)) {
            return companyMapper.idToDto((String) sourceFieldValue);
        }
        throw new IllegalArgumentException("Cannot map " + sourceFieldValue + " to " + destinationClass);
    }

    @Autowired
    public void setCompanyMapper(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
    }
}
