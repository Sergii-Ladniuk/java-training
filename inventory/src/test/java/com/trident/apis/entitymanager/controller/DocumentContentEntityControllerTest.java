package com.trident.apis.entitymanager.controller;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.config.AppConfiguration;
import com.trident.apis.entitymanager.model.DocumentContent;
import com.trident.apis.entitymanager.model.Port;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.repository.CruiseVoyageItineraryCouchbaseRepository;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.test.MockMvcTestBase;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static com.trident.apis.entitymanager.MockDataProvider.getDocumentContentId;
import static com.trident.apis.entitymanager.MockDataProvider.getPortId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AppConfiguration.class)
@Category(CouchbaseIntegrationTest.class)
public class DocumentContentEntityControllerTest extends MockMvcTestBase{

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/document-content";

    @Autowired
    RepositoryDecorator<DocumentContent> documentContentRepository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.DOC_CONTENT);
        mockDataProvider.insertMock(MockDataProvider.MockType.DOC_CONTENT, MOCK_COUNT);
        Iterable<DocumentContent> docContent = documentContentRepository.findAll();
        assert docContent != null;
        assert Iterators.size(docContent.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.DOC_CONTENT);
    }

    @Test
    public void portById() throws Exception {
        int id = 1;
        mvc.perform(get(BASE_PATH + "/{id}", getDocumentContentId(id)))
                .andDo(mvcResult -> System.out.println("Result: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.id", is(getDocumentContentId(id))))
                .andExpect(jsonPath("$.content", is("Content"+id)));
    }

    @Test
    public void insertDocumentContent() throws Exception {
        DocumentContent documentContent = MockDataProvider.createDocContent(4);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(documentContent))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Result: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(documentContentRepository.findOne(documentContent.getId())
                                            .getContent(),
                is(documentContent.getContent()));
    }

    @Test
    public void deleteAllDocumentContent() throws Exception {
        mvc.perform(delete(BASE_PATH))
                .andExpect(status().isOk());
        assertThat(Iterables.size(documentContentRepository.findAll()), is(0));
    }
}
