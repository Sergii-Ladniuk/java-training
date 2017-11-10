package com.trident.apis.entitymanager.service;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.Country;
import com.trident.shared.immigration.dto.apis.port.CountryDto;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchIdInCriteria;
import com.trident.shared.immigration.repository.criteria.SearchFieldInCriteria;
import org.dozer.CustomConverter;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CountryMapper {

    @Autowired
    private RepositoryDecorator<Country> countryRepository;

    public CountryDto idToDto(String countryId) {
        if (countryId == null) {
            return null;
        }
        Country country = countryRepository.findOne(countryId);
        return mapCountryToDto(country);
    }

    private CountryDto mapCountryToDto(Country country) {
        CountryDto result = null;
        if (country != null) {
            result = new CountryDto();
            Mapper mapper = new DozerBeanMapper();
            mapper.map(country, result);
        }
        return result;
    }

    public List<CountryDto> idsToDtos(List<String> countryIds) {
        List<Country> countries = countryRepository.findAll(new SearchCriteriaList(new SearchIdInCriteria(countryIds)));
        Map<String, Country> idToCountryMap = countries.stream().collect(Collectors.toMap(CouchbaseEntityWithId::getId, Function.identity()));
        return countryIds.stream().map(idToCountryMap::get).map(this::mapCountryToDto).collect(Collectors.toList());
    }

    public CountryDto[] idsToDtos(String[] countryIds) {
        if (countryIds == null) {
            return null;
        }
        return idsToDtos(Arrays.asList(countryIds)).toArray(new CountryDto[0]);
    }

    public List<String> codesToIds(List<String> codes) {
        List<Country> countries = countryRepository.findAll(ImmutableList.of("code"), new SearchCriteriaList(new SearchFieldInCriteria("code", codes)));
        return countries.stream().map(CouchbaseEntityWithId::getId).collect(Collectors.toList());
    }

}
