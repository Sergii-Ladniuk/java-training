package com.trident.apis.entitymanager.controller;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisSubmission;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.dto.apis.ApisSubmissionStatus;
import com.trident.shared.immigration.repository.RepositoryDecorator;
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

import static com.trident.apis.entitymanager.MockDataProvider.getSubmissionId;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Category(CouchbaseIntegrationTest.class)
public class ApisSubmissionEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;

    private static final String BASE_PATH = "/api/v1/apis/submissions";

    @Autowired
    private RepositoryDecorator<ApisSubmission> repository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_SUBMISSION, MOCK_COUNT);
        Iterable<ApisSubmission> mocks = repository.findAll();
        assert mocks != null;
        assert Iterators.size(mocks.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
    }

    @Test
    public void loadAll() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void loadAllWithShortModel() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/?" + ControllerConstants.PARAM_FULL_MODEL+ "=false"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]", Matchers.not(Matchers.hasKey("templates"))))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]", Matchers.not(Matchers.hasKey("documents"))))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]", Matchers.not(Matchers.hasKey("knowledge"))));
    }

    @Test
    public void findOne() throws Exception {
        final int id = 1;
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getSubmissionId(id)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(getSubmissionId(id))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ruleName", Matchers.equalTo("ruleName" + id)));
    }

    @Test
    public void insertSubmission() throws Exception {
        int id = MOCK_COUNT + 1;
        ApisSubmission apisSubmission = mockDataProvider.createApisSubmission(id);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(apisSubmission))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(repository.findOne(getSubmissionId(id)).getId(), Matchers.is(getSubmissionId(id)));
    }

    @Test
    public void updateSubmission() throws Exception {
        final int idToUpdate = 1;
        final long updatedStatusDate = System.currentTimeMillis();
        final String updatedPortName = "newPortName";
        ApisSubmission apisSubmission = repository.findOne(getSubmissionId(1));
        apisSubmission.setStatusDate(updatedStatusDate);
        apisSubmission.setPortName(updatedPortName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                .content(json(apisSubmission))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        ApisSubmission updatedApisSubmission = repository.findOne(getSubmissionId(idToUpdate));
        MatcherAssert.assertThat(updatedApisSubmission.getStatusDate(), Matchers.is(updatedStatusDate));
        MatcherAssert.assertThat(updatedApisSubmission.getPortName(), Matchers.is(updatedPortName));
    }

    @Test
    public void deleteSubmission() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getSubmissionId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(Iterables.size(repository.findAll()), Matchers.is(2)); // TODO check history
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String apisSubmissionString = mvc.perform(get(BASE_PATH + "/" + getSubmissionId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        ApisSubmission apisSubmission = objectMapper.readValue(apisSubmissionString, ApisSubmission.class);
        ApisSubmission apisSubmissionStale = objectMapper.readValue(apisSubmissionString, ApisSubmission.class);
        apisSubmission.setStatus(ApisSubmissionStatus.CANCELED);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(apisSubmission))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        apisSubmissionStale.setStatus(ApisSubmissionStatus.KNOWLEDGE_READY);
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(apisSubmission))
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
        String path = "/page?page=0&size=2&sort=portName,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].portName", Matchers.is("portName2")));
    }

    @Test
    public void paginationAndSortWorksWithShortModel() throws Exception {
        String path = "/page?page=0&size=2&sort=portName,DESC&" + ControllerConstants.PARAM_FULL_MODEL+ "=false";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].portName", Matchers.is("portName2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0]", Matchers.hasKey("ruleName")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0]", Matchers.not(Matchers.hasKey("templates"))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0]", Matchers.not(Matchers.hasKey("documents"))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0]", Matchers.not(Matchers.hasKey("knowledge"))));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        final long expectedStartDate = 50000;
        ApisSubmission submission = mockDataProvider.createApisSubmission(MOCK_COUNT);
        submission.setStartDate(expectedStartDate);
        repository.save(submission, null);
        String path = "?sort=portId,DESC&dateFrom=45000&dateTo=55000";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].portId", Matchers.equalTo("portId" + MOCK_COUNT)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedPortId = "newPortId";
        String expectedUser = "historyUser";
        ApisSubmission modifiedApisSubmission = repository.findOne(getSubmissionId(1));
        modifiedApisSubmission.setPortId(expectedPortId);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedApisSubmission.getId())
                .content(json(modifiedApisSubmission))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedApisSubmission.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.portId", Matchers.is(expectedPortId)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }

}
