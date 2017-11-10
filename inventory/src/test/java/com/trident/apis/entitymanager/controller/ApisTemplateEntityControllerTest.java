package com.trident.apis.entitymanager.controller;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.apis.entitymanager.repository.ApisTemplateCouchbaseRepository;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
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

import java.util.List;
import java.util.stream.Collectors;

import static com.trident.apis.entitymanager.MockDataProvider.getRuleId;
import static com.trident.apis.entitymanager.MockDataProvider.getTemplateId;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Category(CouchbaseIntegrationTest.class)
public class ApisTemplateEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/templates";

    @Autowired
    private ApisTemplateCouchbaseRepository apisTemplateCouchbaseRepository;

    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_TEMPLATE);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_RULE, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_TEMPLATE, MOCK_COUNT);
        Iterable<ApisTemplate> templates = apisTemplateCouchbaseRepository.findAll();
        assert templates != null;
        assert Iterators.size(templates.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_TEMPLATE);
    }

    @Test
    public void listAllTemplates() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void listTemplateNames() throws Exception {
        List<ApisTemplate> templateList = apisTemplateCouchbaseRepository.findAll();
        String ids = templateList.stream().map(CouchbaseEntityWithId::getId).collect(Collectors.joining(","));
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/namesByIds?id=" + ids))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0]", Matchers.not(Matchers.hasKey("description"))));

    }

    @Test
    public void templateById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getTemplateId(1)))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(getTemplateId(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("templateName1")));
    }

    @Test
    public void insertTemplate() throws Exception {
        int templateNumber = MOCK_COUNT + 1;
        ApisTemplate template = new ApisTemplate();
        template.setId(getTemplateId(templateNumber));
        template.setName("templateName" + templateNumber);
        template.setContent("content" + templateNumber);
        template.setType(DocumentOutputType.EXCEL);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(template))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(apisTemplateCouchbaseRepository.findOne(getTemplateId(templateNumber)).getName(), Matchers.is("templateName" + templateNumber));
    }

    @Test
    public void updateTemplate() throws Exception {
        int templateNumberToUpdate = MOCK_COUNT + 1;
        int templateNumberToInsert = MOCK_COUNT + 2;
        ApisTemplate template = new ApisTemplate();
        template.setId(getTemplateId(templateNumberToUpdate));
        template.setName("templateName" + templateNumberToInsert);
        template.setContent("content" + templateNumberToInsert);
        template.setType(DocumentOutputType.EXCEL);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(template))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(apisTemplateCouchbaseRepository.findOne(getTemplateId(templateNumberToUpdate)).getName(), Matchers.is("templateName" + templateNumberToInsert));
    }

    @Test
    public void deleteTemplate() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/apis/rules/" + getRuleId(1)));
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/apis/rules/" + getRuleId(2)));
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getTemplateId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        MatcherAssert.assertThat(Iterables.size(apisTemplateCouchbaseRepository.findAll()), Matchers.is(2));
    }

    @Test
    public void deleteTemplateWithException() throws Exception {
        try {
            mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getTemplateId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
        }

    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String apisTemplateString = mvc.perform(get(BASE_PATH + "/" + getTemplateId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        ApisTemplate apisTemplate = objectMapper.readValue(apisTemplateString, ApisTemplate.class);
        ApisTemplate apisTemplateStale = objectMapper.readValue(apisTemplateString, ApisTemplate.class);
        apisTemplate.setName("ApisTemplateDto new value");
//        apisTemplate.setType(DocumentOutputType.EXCEL);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(apisTemplate))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        apisTemplateStale.setName("ApisTemplateDto stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(apisTemplate))
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
    public void itShouldNotSaveTemplateWithExistingNameOnUpdate() throws Exception {
        ApisTemplate apisTemplate = apisTemplateCouchbaseRepository.findOne(getTemplateId(1));
        apisTemplate.setName("templateName0");
        try {
            mvc.perform(put(BASE_PATH)
                    .content(json(apisTemplate))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSaveTemplateWithExistingNameOnInsert() throws Exception {
        ApisTemplate apisTemplate = new ApisTemplate();
        apisTemplate.setName("templateName0");
        apisTemplate.setContent("Some content");
        apisTemplate.setType(DocumentOutputType.EXCEL);
        try {
            mvc.perform(post(BASE_PATH)
                    .content(json(apisTemplate))
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
                .andExpect(jsonPath("$.content[0].name", Matchers.is("templateName2")));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        final String expectedName = "Some name";
        ApisTemplate apisTemplate = new ApisTemplate();
        apisTemplate.setId(getTemplateId(5));
        apisTemplate.setName(expectedName);
        apisTemplate.setType(DocumentOutputType.PLAIN);
        apisTemplate.setContent("Some content");
        apisTemplateCouchbaseRepository.save(apisTemplate, "someUser");

        String path = "?sort=name,DESC&name=ome name";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].name", Matchers.equalTo(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        ApisTemplate modifiedApisTemplate = apisTemplateCouchbaseRepository.findOne(getTemplateId(1));
        String expectedName = "new name";
        modifiedApisTemplate.setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedApisTemplate.getId())
                .content(json(modifiedApisTemplate))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedApisTemplate.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.name", Matchers.is(expectedName)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }

    @Test
    public void testPlainNotes() throws Exception {
        ApisTemplate template = apisTemplateCouchbaseRepository.findOne(getTemplateId(1));
        template.setName("test");
        template.setNotes("my new note");
        template.setType(DocumentOutputType.PLAIN);
        template.setRichFormatNotes(false);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, template.getId())
                .content(json(template))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.notes").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.noteId", Matchers.any(String.class)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.richFormatNotes", Matchers.equalTo(false)));
    }

}