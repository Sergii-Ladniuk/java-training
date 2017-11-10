package com.trident.apis.entitymanager.service;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.Country;
import com.trident.shared.immigration.dto.apis.port.CountryCode;
import com.trident.shared.immigration.dto.apis.port.CountryDto;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class CountryToIdMapperTest {

    @Mock
    private RepositoryDecorator<Country> countryRepository;
    @InjectMocks
    private CountryMapper mapper;

    @Test
    public void testIdToCode() throws Exception {
        String expectedId = "expectedId";
        String expectedCode = CountryCode.UKRAINE.getCode();
        Mockito.when(countryRepository.findOne(expectedId)).thenReturn(getMockCountry(expectedId, expectedCode));
        CountryDto result = mapper.idToDto(expectedId);
        assertThat(result.getCode(), Matchers.is(expectedCode));
    }

    @Test
    public void testIdsToCodesList() throws Exception {
        String[] countryIds = {"id1", "id2"};
        List<String> countryCodes = ImmutableList.of(CountryCode.UKRAINE.getCode(), CountryCode.UNITED_STATES.getCode());
        List<Country> countries = getMockCountries(countryIds, countryCodes.toArray(new String[0]));
        Mockito.when(countryRepository.findAll(Mockito.any(SearchCriteriaList.class))).thenReturn(countries);
        List<CountryDto> codes = mapper.idsToDtos(Arrays.asList(countryIds));
        assertThat(codes.stream().map(CountryDto::getCode).collect(Collectors.toList()), Matchers.equalTo(countryCodes));
    }

    @Test
    public void testIdsToCodesWithUnknownCountries() {
        String[] countryIds = {"id1", "id2"};
        String[] countryCodes = new String[] {CountryCode.UKRAINE.getCode(), CountryCode.UNITED_STATES.getCode()};
        List<Country> countries = ImmutableList.of(getMockCountry(countryIds[0], countryCodes[0]));
        Mockito.when(countryRepository.findAll(Mockito.any(SearchCriteriaList.class))).thenReturn(countries);
        List<CountryDto> codes = mapper.idsToDtos(Arrays.asList(countryIds));
        assertThat(codes.size(), Matchers.is(2));
        assertThat(codes.get(1), Matchers.nullValue());
    }

    @Test
    public void testIdsToCodesArray() throws Exception {
        String[] countryIds = {"id1", "id2"};
        String[] countryCodes = new String[] {CountryCode.UKRAINE.getCode(), CountryCode.UNITED_STATES.getCode()};
        List<Country> countries = getMockCountries(countryIds, countryCodes);
        Mockito.when(countryRepository.findAll(Mockito.any(SearchCriteriaList.class))).thenReturn(countries);
        CountryDto[] codes = mapper.idsToDtos(countryIds);
        assertThat(Arrays.stream(codes).map(CountryDto::getCode).toArray(), Matchers.equalTo(countryCodes));
    }

    private Country getMockCountry(String id, String code) {
        Country country = new Country();
        country.setId(id);
        country.setCode(code);
        return country;
    }

    private List<Country> getMockCountries(String[] ids, String[] codes) {
        if (ids.length != codes.length) { throw new IllegalArgumentException("ids.length != codes.length"); }
        return IntStream.range(0, ids.length).mapToObj(i -> getMockCountry(ids[i], codes[i])).collect(Collectors.toList());
    }

}