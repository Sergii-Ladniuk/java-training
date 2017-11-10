package com.trident.apis.entitymanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.Country;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.shared.immigration.dto.tdrs.TdrsRuleDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.DocumentNode;
import com.trident.shared.immigration.dto.tdrs.recommendation.LogicNode;
import com.trident.shared.immigration.dto.tdrs.recommendation.LogicNodeType;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import static com.trident.apis.entitymanager.MockDataProvider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Category(CouchbaseIntegrationTest.class)
public class TdrsRuleEntityControllerTest extends MockMvcTestBase {

    public static final int MOCK_COUNT = 3;
    public static final String BASE_PATH = "/api/v1/tdrs/rules";

    @Autowired
    private RepositoryDecorator<TdrsRule> tdrsRuleRepository;
    @Autowired
    private RepositoryDecorator<Country> countryRepository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
        mockDataProvider.insertMock(MockDataProvider.MockType.TDRS_RULE, MOCK_COUNT);
        mockDataProvider.insertMock(MockType.COUNTRY, MOCK_COUNT+3);
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
    }

    @Test
    public void all() throws Exception {
        mvc.perform(get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(MOCK_COUNT)));
    }

    @Test
    public void byId() throws Exception {
        mvc.perform(get(BASE_PATH + "/{id}", getTdrsRuleId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.id", is(getTdrsRuleId(1))))
                .andExpect(jsonPath("$.name", is(TDRS_RULE_NAME + "1")))
                .andExpect(jsonPath("$.destinationCountry.code", notNullValue()))
                .andExpect(jsonPath("$.citizenshipCountries[0].code", notNullValue()))
                .andExpect(jsonPath("$.ordinal", is(1)))
                .andExpect(jsonPath("$.recommendation.type", is("OR")))
                .andExpect(jsonPath("$.recommendation.list[0].document.name", notNullValue()))
                .andExpect(jsonPath("$.recommendation.list[1].document.description", notNullValue()));
    }

    @Test
    public void insert() throws Exception {
        TdrsRuleDto tdrsRuleDto = mockDataProvider.createTdrsRuleDto(4, LogicNodeType.AND);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(tdrsRuleDto))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        TdrsRule tdrsRuleResponse = tdrsRuleRepository.findOne(tdrsRuleDto.getId());
        assertThat(tdrsRuleResponse.getName(), is(tdrsRuleDto.getName()));
        assertThat(tdrsRuleResponse.getRecommendation(), instanceOf(LogicNode.class));
        assertThat(((DocumentNode)((LogicNode)tdrsRuleResponse.getRecommendation()).getList().toArray()[0]).getDocumentId(), is("tdrsDocumentId40"));
    }

    @Test
    public void update() throws Exception {
        MvcResult mvcGetResult = mvc.perform(get(BASE_PATH + "/{id}", getTdrsRuleId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn();
        String response = mvcGetResult.getResponse().getContentAsString();
        TdrsRuleDto tdrsRuleResponse = new ObjectMapper().readValue(response, TdrsRuleDto.class);

        String expectedName = "expectedName";
        tdrsRuleResponse.setName(expectedName);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(tdrsRuleResponse))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        TdrsRule tdrsRuleExpected = tdrsRuleRepository.findOne(tdrsRuleResponse.getId());
        assertThat(tdrsRuleExpected.getName(), is(expectedName));
        assertThat(tdrsRuleExpected.getRecommendation(), instanceOf(LogicNode.class));
        assertThat(((DocumentNode)((LogicNode)tdrsRuleExpected.getRecommendation()).getList().toArray()[0]).getDocumentId(), is("tdrsDocumentId10"));
    }

    @Test
    public void deleteById() throws Exception {
        mvc.perform(delete(BASE_PATH + "/{id}", getTdrsRuleId(1)))
                .andExpect(status().isOk());
        assertThat(Iterables.size(tdrsRuleRepository.findAll()), is(2));
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String entityString = mvc.perform(get(BASE_PATH + "/{id}", getTdrsRuleId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        TdrsRule tdrsRule = objectMapper.readValue(entityString, TdrsRule.class);
        TdrsRule tdrsRuleStale = objectMapper.readValue(entityString, TdrsRule.class);
        tdrsRule.setName("tdrsRule new value");
        mvc.perform(
                put(BASE_PATH)
                        .content(json(tdrsRule))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        tdrsRuleStale.setName("tdrsRule stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(tdrsRule))
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
    public void itShouldNotSaveWithSameNameWhenUpdate() throws Exception {
        TdrsRule tdrsRule = tdrsRuleRepository.findOne(getTdrsRuleId(1));
        tdrsRule.setName(TDRS_RULE_NAME + "0");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(tdrsRule))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSaveWithSameNameWhenInsert() throws Exception {
        TdrsRuleDto tdrsRuleDto = mockDataProvider.createTdrsRuleDto(5, LogicNodeType.EMPTY);
        tdrsRuleDto.setName(TDRS_RULE_NAME + "1");
        try {
            mvc.perform(
                    post(BASE_PATH)
                            .content(json(tdrsRuleDto))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                            .header(ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .andExpect(jsonPath("$.totalElements", is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is(TDRS_RULE_NAME + "2")));
    }

    @Test
    public void searchByNameShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC&name=e2"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is(TDRS_RULE_NAME + "2")));
    }

    @Test
    public void searchByDestinationCountryIdShouldWork() throws Exception {
        String destinationCountryId = getCountryId(2);
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC&destinationCountryId=" + destinationCountryId))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].destinationCountry.id", is(destinationCountryId)));
    }

    @Test
    public void searchByNameAndSortShouldWorkWithoutPaging() throws Exception {
        final String expectedName = "Some name";
        TdrsRule tdrsRule = mockDataProvider.createTdrsRule(MOCK_COUNT + 1, LogicNodeType.OR);
        tdrsRule.setName(expectedName);
        tdrsRuleRepository.save(tdrsRule, DEFAULT_USER_ID);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&name=ome name"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(expectedName)));
    }

    @Test
    public void searchByCountryIdAndSortShouldWorkWithoutPaging() throws Exception {
        TdrsRule tdrsRule = mockDataProvider.createTdrsRule(MOCK_COUNT + 1, LogicNodeType.OR);
        final String destinationCountryId = tdrsRule.getDestinationCountryId();
        tdrsRuleRepository.save(tdrsRule, DEFAULT_USER_ID);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&destinationCountryId=" + destinationCountryId))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinationCountry.id", is(destinationCountryId)));
    }

    @Test
    public void searchByCountryCodesAndSortShouldWorkWithoutPaging() throws Exception {
        TdrsRule tdrsRule = mockDataProvider.createTdrsRule(MOCK_COUNT + 1, LogicNodeType.OR);
        final String destinationCountryCode = countryRepository.findOne(tdrsRule.getDestinationCountryId()).getCode();
        tdrsRuleRepository.save(tdrsRule, DEFAULT_USER_ID);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&destinationCountryCodes=" + destinationCountryCode))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].destinationCountry.id", is(tdrsRule.getDestinationCountryId())))
                .andExpect(jsonPath("$[0].id", is(tdrsRule.getId())));
    }

    @Test
    public void checkHistory() throws Exception { // TODO fix test
        String expectedUser = "historyUser";
        MvcResult mvcGetResult = mvc.perform(get(BASE_PATH + "/{id}", getTdrsRuleId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn();
        String response = mvcGetResult.getResponse().getContentAsString();
        TdrsRule modifiedEntity = new ObjectMapper().readValue(response, TdrsRule.class);

//        TdrsRule modifiedEntity = tdrsRuleRepository.findOne(getTdrsRuleId(1));
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