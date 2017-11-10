package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.model.ApisRule;
import com.trident.apis.entitymanager.model.CodeDictionary;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.test.MockMvcTestBase;
import org.aopalliance.reflect.Code;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.trident.apis.entitymanager.MockDataProvider.getCodeDictioinaryId;
import static com.trident.apis.entitymanager.MockDataProvider.getPortId;
import static com.trident.apis.entitymanager.MockDataProvider.getRuleId;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.contains;

@Category(CouchbaseIntegrationTest.class)
public class CodeDictionaryEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/codedictionary";

    @Autowired
    private RepositoryDecorator<CodeDictionary> repositoryDecorator;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    public void setup() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.CODE_DICTIONARY);
        mockDataProvider.insertMock(MockDataProvider.MockType.CODE_DICTIONARY, MOCK_COUNT);
        Iterable<CodeDictionary> savedCodeDictionaries = repositoryDecorator.findAll();
        assertThat(Iterators.size(savedCodeDictionaries.iterator()), is(MOCK_COUNT));
    }

    @After
    public void tearDown() {
        mockDataProvider.cleanDb(MockDataProvider.MockType.CODE_DICTIONARY);
    }

    @Test
    public void list() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void getById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getCodeDictioinaryId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", is(getCodeDictioinaryId(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.codes", Matchers.equalTo(ImmutableList.of("11", "12", "13"))));
    }

    @Test
    public void insert() throws Exception {
        CodeDictionary mock = mockDataProvider.createCodeDictionary(3);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH + "/" + mock.getId())
                .content(json(mock))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(mock.getId())));
        assertThat(Iterators.size(repositoryDecorator.findAll().iterator()), is(MOCK_COUNT + 1));
        assertThat(repositoryDecorator.findOne(mock.getId()).getCodes(), Matchers.hasItems("31", "32", "33"));
    }

    @Test
    public void insertWithoutId() throws Exception {
        CodeDictionary mock = mockDataProvider.createCodeDictionary(3);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(mock))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(mock.getId())));
        assertThat(Iterators.size(repositoryDecorator.findAll().iterator()), is(MOCK_COUNT + 1));
        assertThat(repositoryDecorator.findOne(mock.getId()).getCodes(), Matchers.hasItems("31", "32", "33"));
    }


    @Test
    public void update() throws Exception {
        CodeDictionary mock = repositoryDecorator.findOne(getCodeDictioinaryId(1));
        String expected = "testSet";
        mock.setCodes(ImmutableSet.of(expected));
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH + "/" + mock.getId())
                .content(json(mock))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(mock.getId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.codes", is(ImmutableList.of(expected))));
    }

    @Test
    public void delete() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getCodeDictioinaryId(1)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(Iterators.size(repositoryDecorator.findAll().iterator()), is(MOCK_COUNT - 1));
    }

    @Test
    public void optimisticLockingWorks() throws Exception {
        CodeDictionary mock = repositoryDecorator.findOne(getCodeDictioinaryId(1));
        CodeDictionary mockStale = repositoryDecorator.findOne(getCodeDictioinaryId(1));
        mock.setCodes(ImmutableSet.of("mock"));
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH + "/" + mock.getId())
                .content(json(mock))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mock.setCodes(ImmutableSet.of("stale"));
        try {
            mvc.perform(MockMvcRequestBuilders.put(BASE_PATH + "/" + mock.getId())
                    .content(json(mockStale))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                    .andExpect(MockMvcResultMatchers.status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

}