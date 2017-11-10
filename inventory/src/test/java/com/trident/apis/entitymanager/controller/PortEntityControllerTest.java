package com.trident.apis.entitymanager.controller;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.apis.entitymanager.model.Port;
import com.trident.apis.entitymanager.repository.PortCouchbaseRepository;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
public class PortEntityControllerTest extends MockMvcTestBase {

    public static final int INITIAL_PORT_COUNT = 3;
    public static final String BASE_PATH = "/api/v1/ports";

    @Autowired
    private PortCouchbaseRepository portRepository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.CVI);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
        mockDataProvider.cleanDb(MockDataProvider.MockType.PORT);

        mockDataProvider.insertMock(MockDataProvider.MockType.PORT, INITIAL_PORT_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_RULE, INITIAL_PORT_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_SUBMISSION, INITIAL_PORT_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.CVI, INITIAL_PORT_COUNT);
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.CVI);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_RULE);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
        mockDataProvider.cleanDb(MockDataProvider.MockType.PORT);

    }

    @Test
    public void allPorts() throws Exception {
        mvc.perform(get(BASE_PATH))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(INITIAL_PORT_COUNT)));
    }

    @Test
    public void portById() throws Exception {
        mvc.perform(get(BASE_PATH + "/{portId}", getPortId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.id", is(getPortId(1))))
                .andExpect(jsonPath("$.name", is("portName1")));
    }

    @Test
    public void insertPort() throws Exception {
        Port port = mockDataProvider.createPort(4);
        mvc.perform(
                post(BASE_PATH)
                        .content(json(port))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(portRepository.findOne(port.getId()).getName(), is(port.getName()));
    }

    @Test
    public void updatePort() throws Exception {
        Port port = portRepository.findOne(getPortId(1));
        String expectedName = "expectedPortName";
        port.setName(expectedName);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(port))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        assertThat(portRepository.findOne(port.getId()).getName(), is(expectedName));
    }

    @Test
    public void portsByCountry() throws Exception {
        mvc.perform(get("/api/v1/countries/{countryId}/ports", getCountryId(0)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("portName0")));
    }

    @Test
    public void portsByCountryWithSort() throws Exception {
        Port port2 = portRepository.findOne(getPortId(2));
        port2.setCountryId(getCountryId(0));
        portRepository.save(port2, null);
        mvc.perform(get("/api/v1/countries/{countryId}/ports?sort=name,DESC", getCountryId(0)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("portName2")));
    }

    @Test
    public void deletePort() throws Exception {
        mvc.perform(delete("/api/v1/apis/submissions/{apisSubmissionId}", getSubmissionId(1)));
        mvc.perform(delete("/api/v1/apis/cvi/{cviId}", getCviId(0)));
        mvc.perform(delete("/api/v1/apis/cvi/{cviId}", getCviId(1)));
        mvc.perform(delete("/api/v1/apis/cvi/{cviId}", getCviId(2)));
        mvc.perform(delete("/api/v1/apis/rules/{rulesId}", getRuleId(0)));
        mvc.perform(delete("/api/v1/apis/rules/{rulesId}", getRuleId(1)));
        mvc.perform(delete(BASE_PATH + "/{portId}", getPortId(1)))
                .andExpect(status().isOk());
        assertThat(Iterables.size(portRepository.findAll()), is(2));
    }

    @Test
    public void deletePortWithException() throws Exception {
        try {
            mvc.perform(delete(BASE_PATH + "/{portId}", getPortId(0)))
                    .andExpect(status().isOk());
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
        }

    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String portString = mvc.perform(get(BASE_PATH + "/{portId}", getPortId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Port port = objectMapper.readValue(portString, Port.class);
        Port portStale = objectMapper.readValue(portString, Port.class);
        port.setName("port new value");
        mvc.perform(
                put(BASE_PATH)
                        .content(json(port))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        portStale.setName("port stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(port))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    // expect conflict status
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(409));
        } catch (NestedServletException| OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSavePortsWithSameCodeWhenUpdate() throws Exception {
        Port port = portRepository.findOne(getPortId(1));
        port.setCode("portCode0");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(port))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));
        } catch (NestedServletException| OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Ignore
    @Test
    public void itShouldNotSavePortsWithSameNameWhenUpdate() throws Exception {
        Port port = portRepository.findOne(getPortId(1));
        port.setName("portName0");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(port))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    .andExpect(status().is(409));
        } catch (NestedServletException| OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void itShouldNotSavePortsWithSameCodeWhenInsert() throws Exception {
        Port port = mockDataProvider.createPort(5);
        port.setCountryId("country123");
        port.setCode("portCode0");
        port.setName("new name...");
        try {
            mvc.perform(
                post(BASE_PATH)
                    .content(json(port))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .header(ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
             .andExpect(status().is(409));
        } catch (NestedServletException| OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void pagingAndSortShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(INITIAL_PORT_COUNT)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("portName2")));
    }

    @Test
    public void searchShouldWork() throws Exception {
        mvc.perform(get(BASE_PATH + "/page?page=0&size=2&sort=name,DESC&name=e2"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("portName2")));
    }

    @Test
    public void searchAndSortShouldWorkWithoutPaging() throws Exception {
        final String expectedName = "Some name";
        Port port = mockDataProvider.createPort(INITIAL_PORT_COUNT + 1);
        port.setName(expectedName);
        portRepository.save(port, null);
        mvc.perform(get(BASE_PATH + "?sort=name,DESC&name=ome name"))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        Port modifiedEntity = portRepository.findOne(getPortId(1));
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

    @Test
    public void testPlainNotes() throws Exception {
        Port port = portRepository.findOne(getPortId(1));
        port.setRichFormatNotes(false);
        port.setName("test");
        port.setNotes("my new note");
        port.setRichFormatNotes(false);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, port.getId())
                .content(json(port))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.notes").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.noteId", Matchers.any(String.class)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.richFormatNotes", Matchers.equalTo(false)));
    }

}