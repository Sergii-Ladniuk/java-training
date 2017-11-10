package com.trident.apis.entitymanager.service;

import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipCertificateList;
import org.dozer.CustomConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CertificateListDozerConverter implements CustomConverter {

    private CompanyMapper companyMapper;

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue == null) {
            return null;
        } else if (destinationClass.equals(ShipCertificateList.class) && sourceClass.getCanonicalName().equals(ShipCertificateList.class.getCanonicalName())) {
            return sourceFieldValue;
        } else if (destinationClass.equals(ShipCertificateList.class) && sourceClass.equals(ArrayList.class)) {
            ArrayList arrayList = (ArrayList) sourceFieldValue;
            return ShipCertificateList.of(arrayList);
        }
        throw new IllegalArgumentException("Cannot map " + sourceFieldValue + " to " + destinationClass);
    }

    @Autowired
    public void setCompanyMapper(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
    }
}
