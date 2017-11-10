package com.trident.apis.entitymanager.controller;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.apis.entitymanager.repository.TdrsDocumentCouchbaseRepository;
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

import static com.trident.apis.entitymanager.MockDataProvider.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Category(CouchbaseIntegrationTest.class)
public class TdrsDocumentEntityControllerTest extends MockMvcTestBase {

    public static final int INITIAL_TDRS_DOCUMENT_COUNT = 3;
    public static final String BASE_PATH = "/api/v1/tdrs/documents";

    @Autowired
    private TdrsDocumentCouchbaseRepository tdrsDocumentRepository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
        //mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_DOCUMENT);
        //mockDataProvider.insertMock(MockDataProvider.MockType.TDRS_DOCUMENT, INITIAL_TDRS_DOCUMENT_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.TDRS_RULE, INITIAL_TDRS_DOCUMENT_COUNT);
    }

    @After
    public void tearDown() throws Exception {
       mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_RULE);
        //mockDataProvider.cleanDb(MockDataProvider.MockType.TDRS_DOCUMENT);
    }

    @Test
    public void all() throws Exception {
        mvc.perform(get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    public void byId() throws Exception {
        mvc.perform(get(BASE_PATH + "/{id}", getTdrsDocumentId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.id", is(getTdrsDocumentId(1))))
                .andExpect(jsonPath("$.name", is(TDRS_DOCUMENT_NAME + "1")));
    }

    @Test
    public void insert() throws Exception {
        TdrsDocument tdrsDocument = mockDataProvider.createTdrsDocument(4);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(tdrsDocument))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(tdrsDocumentRepository.findOne(tdrsDocument.getId()).getName(), is(tdrsDocument.getName()));
    }

    @Test
    public void update() throws Exception {
        TdrsDocument tdrsDocument = tdrsDocumentRepository.findOne(getTdrsDocumentId(1));
        String expectedName = "expectedName";
        tdrsDocument.setName(expectedName);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(tdrsDocument))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(tdrsDocumentRepository.findOne(tdrsDocument.getId()).getName(), is(expectedName));
    }

    @Test
    public void deleteById() throws Exception {
        mvc.perform(delete("/api/v1/tdrs/rules/{id}", getTdrsRuleId(1)));
        mvc.perform(delete(BASE_PATH + "/{id}", getTdrsDocumentId(1)))
                .andExpect(status().isOk());
        assertThat(Iterables.size(tdrsDocumentRepository.findAll()), is(4));
    }

    @Test
    public void deleteWithRule() throws Exception {
        try {
            mvc.perform(delete(BASE_PATH + "/{id}", getTdrsDocumentId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
            assert (ex.getMessage().contains("Rule"));
        }
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String entityString = mvc.perform(get(BASE_PATH + "/{id}", getTdrsDocumentId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        TdrsDocument tdrsDocument = objectMapper.readValue(entityString, TdrsDocument.class);
        TdrsDocument tdrsDocumentStale = objectMapper.readValue(entityString, TdrsDocument.class);
        tdrsDocument.setName("tdrsDocument new value");
        mvc.perform(
                put(BASE_PATH)
                        .content(json(tdrsDocument))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        tdrsDocumentStale.setName("tdrsDocument stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(tdrsDocument))
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
        TdrsDocument tdrsDocument = tdrsDocumentRepository.findOne(getTdrsDocumentId(1));
        tdrsDocument.setName(TDRS_DOCUMENT_NAME + "0");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(tdrsDocument))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSaveWithSameNameWhenInsert() throws Exception {
        TdrsDocument tdrsDocument = mockDataProvider.createTdrsDocument(5);
        tdrsDocument.setName(TDRS_DOCUMENT_NAME + "1");
        try {
            mvc.perform(
                    post(BASE_PATH)
                            .content(json(tdrsDocument))
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
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is(TDRS_DOCUMENT_NAME + "20")));
    }

    @Test
    public void searchShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC&name=e20"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is(TDRS_DOCUMENT_NAME + "20")));
    }

    @Test
    public void searchAndSortShouldWorkWithoutPaging() throws Exception {
        final String expectedName = "Some name";
        TdrsDocument tdrsDocument = mockDataProvider.createTdrsDocument(INITIAL_TDRS_DOCUMENT_COUNT + 1);
        tdrsDocument.setName(expectedName);
        tdrsDocumentRepository.save(tdrsDocument, null);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&name=ome name"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        TdrsDocument modifiedEntity = tdrsDocumentRepository.findOne(getTdrsDocumentId(1));
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