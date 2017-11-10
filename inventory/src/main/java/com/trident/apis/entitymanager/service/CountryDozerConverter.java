package com.trident.apis.entitymanager.service;

import com.trident.shared.immigration.dto.apis.port.CountryDto;
import org.dozer.CustomConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CountryDozerConverter implements CustomConverter {

    private CountryMapper countryMapper;

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue == null) {
            return null;
        } else if (destinationClass.equals(String.class) && sourceClass.equals(CountryDto.class)) {
            CountryDto countryDto = (CountryDto) sourceFieldValue;
            return countryDto.getId();
        } else if (destinationClass.equals(CountryDto.class) && sourceClass.equals(String.class)) {
            return countryMapper.idToDto((String) sourceFieldValue);
        }
        throw new IllegalArgumentException("Cannot map " + sourceFieldValue + " to " + destinationClass);
    }

    @Autowired
    public void setCountryMapper(CountryMapper countryMapper) {
        this.countryMapper = countryMapper;
    }
}
