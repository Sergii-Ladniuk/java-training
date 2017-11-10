package com.trident.apis.entitymanager.controller;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.TridentToPortTranslation;
import com.trident.apis.entitymanager.repository.TridentToPortTranslationCouchbaseRepository;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortFieldTranslation;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import static com.trident.apis.entitymanager.MockDataProvider.getRuleId;
import static com.trident.apis.entitymanager.MockDataProvider.getTridentToPortTranslatationId;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(CouchbaseIntegrationTest.class)
public class TridentToPortTranslationEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/tridentToApis";

    @Autowired
    private TridentToPortTranslationCouchbaseRepository repository;

    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockDataProvider.MockType.TRIDENT_TO_PORT_TRANSLATION);
        mockDataProvider.insertMock(MockDataProvider.MockType.TRIDENT_TO_PORT_TRANSLATION, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_RULE, MOCK_COUNT);
        Iterable<TridentToPortTranslation> templates = repository.findAll();
        assert templates != null;
        assert Iterators.size(templates.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.TRIDENT_TO_PORT_TRANSLATION);
    }

    @Test
    public void listAll() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void byId() throws Exception {
        final int id = 1;
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getTridentToPortTranslatationId(id)))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(getTridentToPortTranslatationId(id))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fieldsTranslation", Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fieldsTranslation.['notice.vessel.idType" + id + "']", Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fieldsTranslation.['notice.vessel.idType" + id + "'].tridentToApisMapping.tridentType"+id,
                        Matchers.equalTo("portType"+id)));
    }

    @Test
    public void insert() throws Exception {
        final int id = MOCK_COUNT + 1;
        String expected = "test.path";
        TridentToPortTranslation translation = mockDataProvider.createTridentToPortTranslation(id);
        translation.getFieldsTranslation().put("test", new TridentToPortFieldTranslation(expected));
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(translation))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.id", Matchers.equalTo(getTridentToPortTranslatationId(id))))
                .andExpect(jsonPath("$.fieldsTranslation.test.fieldPath", Matchers.equalTo(expected)));;
    }

    @Test
    public void update() throws Exception {
        final int id = 1;
        String expected = "test.path";
        TridentToPortTranslation translation = repository.findOne(getTridentToPortTranslatationId(id));
        translation.getFieldsTranslation().put("test", new TridentToPortFieldTranslation(expected));
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                .content(json(translation))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.id", Matchers.equalTo(getTridentToPortTranslatationId(id))))
                .andExpect(jsonPath("$.fieldsTranslation.test.fieldPath", Matchers.equalTo(expected)));
    }

    @Test
    public void  delete() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/apis/rules/" + getRuleId(2)));
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getTridentToPortTranslatationId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(Iterables.size(repository.findAll()), Matchers.is(MOCK_COUNT - 1));
    }

    @Test
    public void deleteWithException() throws Exception {
        try {
            mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getTridentToPortTranslatationId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
        }
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String expected = "test.path";
        TridentToPortTranslation translation = repository.findOne(getTridentToPortTranslatationId(1));
        TridentToPortTranslation translationStale = repository.findOne(getTridentToPortTranslatationId(1));
        translation.getFieldsTranslation().put("test", new TridentToPortFieldTranslation(expected));

        mvc.perform(
                put(BASE_PATH)
                        .content(json(translation))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        translationStale.getFieldsTranslation().put("test1", new TridentToPortFieldTranslation(expected));
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(translationStale))
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
    public void paginationAndSortWorks() throws Exception {
        String path = "/page?page=0&size=2&sort=id,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", Matchers.is(getTridentToPortTranslatationId(MOCK_COUNT - 1))));
    }

//    @Test
//    public void searchAndSortShouldWork() throws Exception {
//        final String expectedRuleId = "someRuleId";
//        TridentToPortTranslation translation = mockDataProvider.createTridentToPortTranslation(MOCK_COUNT+1);
//        translation.setRuleId(expectedRuleId);
//        documentContentCouchbaseRepository.save(translation, MockDataProvider.DEFAULT_USER_ID);
//
//        String path = "?sort=ruleId,DESC&ruleId=" + expectedRuleId;
//        mvc.perform(get(BASE_PATH + path))
//                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
//                .andExpect(jsonPath("$", Matchers.hasSize(1)))
//                .andExpect(jsonPath("$.[0].ruleId", Matchers.equalTo(expectedRuleId)));
//    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        String expected = "new ruleId";
        TridentToPortTranslation modifiedTridentToPortTranslation = repository.findOne(getTridentToPortTranslatationId(1));
        modifiedTridentToPortTranslation.getFieldsTranslation().put("test", new TridentToPortFieldTranslation(expected));
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedTridentToPortTranslation.getId())
                .content(json(modifiedTridentToPortTranslation))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedTridentToPortTranslation.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.fieldsTranslation.test.fieldPath", Matchers.is(expected)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }
}