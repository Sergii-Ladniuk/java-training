package com.trident.apis.entitymanager.controller;

import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.Country;
import com.trident.apis.entitymanager.repository.CountryCouchbaseRepository;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.trident.apis.entitymanager.MockDataProvider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Category(CouchbaseIntegrationTest.class)
public class CountryEntityControllerTest extends MockMvcTestBase {

    public static final int INITIAL_COUNTRY_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/countries";
    @Autowired
    private CountryCouchbaseRepository countryRepository;
    @Autowired
    private MockDataProvider testDataProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
        testDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        testDataProvider.cleanDb(MockDataProvider.MockType.PORT);
        testDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
        testDataProvider.insertMock(MockDataProvider.MockType.COUNTRY, INITIAL_COUNTRY_COUNT);
        testDataProvider.insertMock(MockDataProvider.MockType.TDRS_RULE, INITIAL_COUNTRY_COUNT);
        testDataProvider.insertMock(MockDataProvider.MockType.SHIP_ENTITY, INITIAL_COUNTRY_COUNT);
        testDataProvider.insertMock(MockType.PORT, INITIAL_COUNTRY_COUNT);
    }

    @After
    public void tearDown() throws Exception {
        testDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
        testDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
        testDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        testDataProvider.cleanDb(MockType.PORT);
    }

    @Test
    public void allCountries() throws Exception {
        mvc.perform(get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(INITIAL_COUNTRY_COUNT)));
    }

    @Test
    public void countryById() throws Exception {
        final String expectedId = getCountryId(1);
        mvc.perform(get(BASE_PATH + "/{countryId}", expectedId))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.id", is(expectedId)))
                .andExpect(jsonPath("$.name", is("countryName1")));
    }

    @Test
    public void insertCountry() throws Exception {
        Country country = testDataProvider.createCountry(8);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(country))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(countryRepository.findOne(country.getId()).getName(), is("countryName8"));
        assertThat((countryRepository.getHistoryById(country.getId()).size()), is(1));
    }

    @Test
    public void insertBatchNoRepeatName() throws Exception {
        List<Country> countries =
                ImmutableList.of(MockDataProvider.createCountry(1), MockDataProvider.createCountry(2), MockDataProvider.createCountry(15));
        countries.forEach(country -> country.setId(UUID.randomUUID().toString()));
        mvc.perform(
                post(BASE_PATH + "/list")
                        .content(json(countries))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void insertCountryList() throws Exception {
        List<Country> countries = new ArrayList<>(INITIAL_COUNTRY_COUNT);
        for (int i = 0; i < INITIAL_COUNTRY_COUNT; i++) {
            Country country = testDataProvider.createCountry(INITIAL_COUNTRY_COUNT + i);
            countries.add(country);
        }
        mvc.perform(
                post(BASE_PATH + "/list")
                        .content(json(countries))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(Iterables.size(countryRepository.findAll()), is(INITIAL_COUNTRY_COUNT * 2));
    }

    @Test
    public void updateCountry() throws Exception {
        Country country = countryRepository.findOne(getCountryId(INITIAL_COUNTRY_COUNT - 1));
        String expectedName = "countryName5";
        country.setName(expectedName);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(country))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(countryRepository.findOne(country.getId()).getName(), is(expectedName));
        assertThat((countryRepository.getHistoryById(country.getId()).size()), is(2));
    }

    @Test
    public void deleteCountry() throws Exception {
        mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(0)));
        mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(1)));

        mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(1)));
        mvc.perform(delete("/api/v1/apis/ships/{ship}", getShipId(1)));

        mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(0)));
        mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(2)));
        mvc.perform(delete("/api/v1/apis/rules/{id}", getRuleId(0)));
        mvc.perform(delete("/api/v1/apis/rules/{id}", getRuleId(1)));
        mvc.perform(delete("/api/v1/ports/{port}", getPortId(1)));

        mvc.perform(delete(BASE_PATH + "/{countryId}", getCountryId(1)))
                .andExpect(status().isOk());
        assertThat(Iterables.size(countryRepository.findAll()), is(2));
        assertThat((countryRepository.getHistoryById(getCountryId(1)).size()), is(2));
    }

    @Test
    public void deleteCountryWithTdrsRuleException() throws Exception {
        try {
            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(1)));
            mvc.perform(delete("/api/v1/apis/ships/{ship}", getShipId(1)));

            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(0)));
            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(2)));
            mvc.perform(delete("/api/v1/apis/rules", getRuleId(0)));
            mvc.perform(delete("/api/v1/apis/rules", getRuleId(1)));
            mvc.perform(delete("/api/v1/ports/{port}", getPortId(1)));

            mvc.perform(delete(BASE_PATH + "/{countryId}", getCountryId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
            assert (ex.getMessage().contains("Citizenship"));
            assert (ex.getMessage().contains("Destination"));
        }
    }

    @Test
    public void deleteCountryWithPort() throws Exception {
        try {
            mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(0)));
            mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(1)));

            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(1)));
            mvc.perform(delete("/api/v1/apis/ships/{ship}", getShipId(1)));

            mvc.perform(delete(BASE_PATH + "/{countryId}", getCountryId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
            assert (ex.getMessage().contains("Port"));
        }
    }

    @Test
    public void deleteCountryWithShips() throws Exception {
        try {
            mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(0)));
            mvc.perform(delete("/api/v1/tdrs/rules/{tdrsRule}", getTdrsRuleId(1)));

            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(0)));
            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(1)));
            mvc.perform(delete("/api/v1/apis/cvi/{cvi}", getCviId(2)));
            mvc.perform(delete("/api/v1/apis/rules/{id}", getRuleId(0)));
            mvc.perform(delete("/api/v1/apis/rules/{id}", getRuleId(1)));
            mvc.perform(delete("/api/v1/ports/{port}", getPortId(1)));

            mvc.perform(delete(BASE_PATH + "/{countryId}", getCountryId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
            assert (ex.getMessage().contains("Ship"));
        }
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        Country country = countryRepository.findOne(getCountryId(1));
        Country countryStale = countryRepository.findOne(getCountryId(1));
        country.setName("country new value");
        mvc.perform(
                put(BASE_PATH)
                        .content(json(country))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        countryStale.setName("country stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(countryStale))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    // expect conflict status
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void shouldNotSaveCountriesWithSameName() throws Exception {
        String sameName = "sameName";
        Country country = testDataProvider.createCountry(10);
        country.setName(sameName);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(country))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        country = testDataProvider.createCountry(11);
        country.setName(sameName);
        try {
            mvc.perform(
                    post(BASE_PATH)
                            .content(json(country))
                            .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));

        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void shouldNotSaveCountriesWithSameCode() throws Exception {
        Country country = testDataProvider.createCountry(6);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(country))
                        .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        country.setId("other-id");
        country.setCode("other-code-6");
        try {
            mvc.perform(
                    post(BASE_PATH)
                            .content(json(country))
                            .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));


        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void pagingAndSortShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(INITIAL_COUNTRY_COUNT)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("countryName2")));
    }

    @Test
    public void searchShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC&name=e2"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("countryName2")));
    }

    @Test
    public void searchAndSortShouldWorkWithoutPaging() throws Exception {
        final String expectedName = "Some name";
        Country country = new Country();
        country.setId(getCountryId(INITIAL_COUNTRY_COUNT + 1));
        country.setName(expectedName);
        country.setCode("Some code");
        countryRepository.save(country, null);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&name=ome name"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        Country modifiedEntity = countryRepository.findOne(getCountryId(1));
        String expectedName = "new name";
        modifiedEntity.setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedEntity.getId())
                .content(json(modifiedEntity))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedEntity.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.name", Matchers.is(expectedName)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }
}