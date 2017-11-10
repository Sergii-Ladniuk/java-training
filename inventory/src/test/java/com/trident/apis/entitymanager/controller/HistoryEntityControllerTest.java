package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.Country;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Created by dimzul on 3/27/17.
 */
@Category(CouchbaseIntegrationTest.class)
public class HistoryEntityControllerTest extends MockMvcTestBase {

    private static final String BASE_PATH = "/api/v1/history";
    private static final int MOCK_COUNT = 3;
    private static final String USER_ID = "testUserId";

    @Autowired
    private MockDataProvider mockDataProvider;
    @Autowired
    private RepositoryDecorator<Country> repositoryDecorator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.HISTORY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
        mockDataProvider.insertMock(MockDataProvider.MockType.COUNTRY, MOCK_COUNT);
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
    }

    @Test
    public void paginationAndSortWorks() throws Exception {
        String expectedName1 = "updated_country_1";
        String expectedName2 = "updated_country_2";
        addHistory(expectedName1, expectedName2);
        String path = "?page=0&size=2&sort=updateDate,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT + 2)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(3)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].entity.name", Matchers.is(expectedName2)))
                .andExpect(jsonPath("$.content[1].entity.name", Matchers.is(expectedName1)));
    }

    @Test
    public void searchAndSortShouldWorkWithoutPaging() throws Exception {
        String expectedName1 = "updated_country_1";
        String expectedName2 = "updated_country_2";
        addHistory(expectedName1, expectedName2);
        mvc.perform(get(BASE_PATH + "?sort=updateDate,DESC&userId=" + USER_ID))
                .andDo(mvcResult -> System.out.println("Response:\n" + mvcResult.getResponse().getContentAsString()))
//                .andExpect(jsonPath("$.size", Matchers.is(1)))
                .andExpect(jsonPath("$.totalElements", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].entity.name", is(expectedName2)));
    }

    @Test
    public void reducedModel() throws Exception {
        String expectedName1 = "updated_country_1";
        String expectedName2 = "updated_country_2";
        addHistory(expectedName1, expectedName2);
        mvc.perform(get(BASE_PATH + "?size=1&userId=" + USER_ID + "&" + ControllerConstants.PARAM_FULL_MODEL + "=false"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.size", Matchers.is(1)))
                .andExpect(jsonPath("$.content[0].entity.code").doesNotExist())
                .andExpect(jsonPath("$.content[0].entity.name").isNotEmpty())
                .andExpect(jsonPath("$.content[0].updateDate").isNotEmpty())
                .andExpect(jsonPath("$.content[0].writeMode").isNotEmpty())
                .andExpect(jsonPath("$.content[0].entityId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].userId").isNotEmpty());
    }

    private void addHistory(String name1, String name2) {
        Country country = repositoryDecorator.findOne(MockDataProvider.getCountryId(1));
        country.setName(name1);
        repositoryDecorator.save(country, USER_ID);
        country = repositoryDecorator.findOne(MockDataProvider.getCountryId(1));
        country.setName(name2);
        repositoryDecorator.save(country, USER_ID);
    }

}