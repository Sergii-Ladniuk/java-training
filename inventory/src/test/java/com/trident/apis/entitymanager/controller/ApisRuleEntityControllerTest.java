package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.shared.immigration.dto.apis.ApisRuleDto;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethodCredentialsType;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethodType;
import com.trident.shared.immigration.exception.ResourceNotFoundException;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import static com.trident.apis.entitymanager.MockDataProvider.*;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(CouchbaseIntegrationTest.class)
@SpringBootTest
public class ApisRuleEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/rules";

    @Autowired
    private RepositoryDecorator<ApisRule> repositoryDecorator;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockType.PORT);
        mockDataProvider.cleanDb(MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockType.APIS_TEMPLATE);
        mockDataProvider.insertMock(MockType.APIS_RULE, MOCK_COUNT);
        mockDataProvider.insertMock(MockType.PORT, MOCK_COUNT+3);
        mockDataProvider.insertMock(MockType.APIS_TEMPLATE, MOCK_COUNT+3);
        mockDataProvider.insertMock(MockType.TRIDENT_TO_PORT_TRANSLATION, MOCK_COUNT+3);
        Iterable<ApisRule> apisRules = repositoryDecorator.findAll();
        assert apisRules != null;
        assert Iterators.size(apisRules.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
    }

    @Test
    public void loadAllApisRules() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void loadApisRuleByPage() throws Exception {
        String path = "/page?page=0&size=2&sort=name,DESC";
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.size", Matchers.is(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id", Matchers.equalTo(getRuleId(2))));
    }

    @Test
    public void searchApisRuleSorted() throws Exception {
        String path = "?sort=name,DESC&name=e2";
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.equalTo("ruleName2")));
    }

    @Test
    public void submissionMethodIsReturned() throws Exception {
        String path = "?sort=name,ASC";
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.equalTo("ruleName0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].submissionMethod.type", Matchers.is(ApisSubmissionMethodType.FTP.toString())))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].submissionMethod.credentials.type",
                        Matchers.is(ApisSubmissionMethodCredentialsType.USERNAME_PASSWORD.toString())));
    }

    @Test
    public void apiRuleById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getRuleId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("ruleName1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.apisPortIds", Matchers.hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.apisPortIds[0]", Matchers.equalTo(getPortId(1))));
    }

    @Test
    public void insertApisRule() throws Exception {
        String id = getRuleId(3);
        String name = "ruleName3";
        ApisRule apisRule = new ApisRule();
        apisRule.setId(id);
        apisRule.setName(name);
        apisRule.setApisPortIds(ImmutableList.of("portId4"));
        apisRule.setApisTemplateIds(ImmutableList.of("templateId4"));
        apisRule.setSubmissionMethod(mockDataProvider.getHttpSubmissionMethod());
        apisRule.setTridentToPortTranslationId(MockDataProvider.getTridentToPortTranslatationId(0));
        apisRule.setRuleType(ApisRuleDto.ApisRuleType.APIS);
        apisRule.setDirection(ApisRuleDto.ApisDirection.ARRIVAL);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(apisRule))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo(name)));
        assert Iterators.size(repositoryDecorator.findAll().iterator()) == MOCK_COUNT + 1;
        assert repositoryDecorator.findOne(id).getName().equals(name);
    }

    @Test
    public void insertApisRuleBadPortId() throws Exception {
        String id = getRuleId(3);
        String name = "ruleName3";
        ApisRule apisRule = new ApisRule();
        apisRule.setId(id);
        apisRule.setName(name);
        apisRule.setApisPortIds(ImmutableList.of("badPortId"));
        apisRule.setApisTemplateIds(ImmutableList.of("templateId4"));
        apisRule.setSubmissionMethod(mockDataProvider.getHttpSubmissionMethod());
        apisRule.setTridentToPortTranslationId(MockDataProvider.getTridentToPortTranslatationId(0));
        apisRule.setRuleType(ApisRuleDto.ApisRuleType.APIS);
        apisRule.setDirection(ApisRuleDto.ApisDirection.ARRIVAL);
        try {
            mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(apisRule))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is5xxServerError());
        } catch (NestedServletException e) {
            assertTrue(e.getCause() instanceof ResourceNotFoundException);
            assertTrue(e.getCause().getMessage().contains("badPortId"));
        }
    }

    @Test
    public void updateExistingApisRule() throws Exception {
        String modifiedApisRuleId = getRuleId(1);
        String expectedName = "new name";
        ApisRule apisRule = repositoryDecorator.findOne(modifiedApisRuleId);
        apisRule.setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                    .content(json(apisRule))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo(expectedName)));
        assert repositoryDecorator.findOne(modifiedApisRuleId).getName().equals(expectedName);
    }

    @Test
    public void updateExistingApisRuleWithId() throws Exception {
        String modifiedApisRuleId = getRuleId(1);
        String expectedName = "new name";
        ApisRule apisRule = repositoryDecorator.findOne(modifiedApisRuleId);
        apisRule.setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedApisRuleId)
                    .content(json(apisRule))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo(expectedName)));
        assert repositoryDecorator.findOne(modifiedApisRuleId).getName().equals(expectedName);
    }

    @Test
    public void deleteApisRule() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getRuleId(1)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        assert Iterators.size(repositoryDecorator.findAll().iterator()) == MOCK_COUNT - 1;
    }

    @Test
    public void optimisticLockingWorks() throws Exception {
        ApisRule apisRule = repositoryDecorator.findOne(getRuleId(1));
        ApisRule apisRuleStale = repositoryDecorator.findOne(getRuleId(1));
        apisRule.setName("name3");
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                    .content(json(apisRule))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        apisRuleStale.setName("name4");
        try {
            mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                    .content(json(apisRuleStale))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                    .andExpect(MockMvcResultMatchers.status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSaveRuleWithExistingNameOnUpdate() throws Exception {
        ApisRule apisRule = repositoryDecorator.findOne(getRuleId(1));
        apisRule.setName("ruleName0");
        try {
            mvc.perform(put(BASE_PATH)
                    .content(json(apisRule))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSaveRuleWithExistingNameOnInsert() throws Exception {
        ApisRule apisRule = repositoryDecorator.findOne(getRuleId(1));
        apisRule.setName("ruleName0");
        try {
            mvc.perform(post(BASE_PATH)
                    .content(json(apisRule))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }


    @Test
    public void paginationAndSortWorks() throws Exception {
        String path = "/page?page=0&size=2&sort=name,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", Matchers.is("ruleName2")));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        final String expectedName = "Some name";
        ApisRule apisRule = new ApisRule();
        apisRule.setId(getTemplateId(5));
        apisRule.setName(expectedName);
        apisRule.setSubmissionMethod(mockDataProvider.getHttpSubmissionMethod());
        apisRule.setApisPortIds(ImmutableList.of(getPortId(MOCK_COUNT), getPortId(MOCK_COUNT+1)));
        apisRule.setApisTemplateIds(ImmutableList.of(getTemplateId(MOCK_COUNT), getTemplateId(MOCK_COUNT+1)));
        repositoryDecorator.save(apisRule, "someUser");

        String path = "?sort=name,DESC&name=ome name";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].name", Matchers.equalTo(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        ApisRule modifiedApisRuleId = repositoryDecorator.findOne(getRuleId(1));
        String expectedName = "new name";
        modifiedApisRuleId.setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedApisRuleId.getId())
                .content(json(modifiedApisRuleId))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedApisRuleId.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.name", Matchers.is(expectedName)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }

}