package com.trident.apis.entitymanager.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.config.AppConfiguration;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.repository.CruiseVoyageItineraryCouchbaseRepository;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.exception.CviAlreadyExistsException;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import static com.trident.apis.entitymanager.MockDataProvider.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AppConfiguration.class)
@Category(CouchbaseIntegrationTest.class)
public class CruiseVoyageItineraryEntityControllerTest extends MockMvcTestBase {
    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/cvi";

    @Autowired
    CruiseVoyageItineraryCouchbaseRepository cviCouchbaseRepository;
    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
        mockDataProvider.cleanDb(MockDataProvider.MockType.CVI);
        mockDataProvider.cleanDb(MockDataProvider.MockType.PORT);
        mockDataProvider.insertMock(MockDataProvider.MockType.SHIP_ENTITY, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.APIS_SUBMISSION, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.CVI, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.PORT, MOCK_COUNT);
        Iterable<CruiseVoyageItinerary> cvis = cviCouchbaseRepository.findAll();
        assert cvis != null;
        assert Iterators.size(cvis.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.APIS_SUBMISSION);
        mockDataProvider.cleanDb(MockDataProvider.MockType.CVI);
        mockDataProvider.cleanDb(MockDataProvider.MockType.PORT);
    }

    @Test
    public void findOneFullById() throws Exception {
        String cviId = getCviId(1);
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + cviId))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(cviId)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brandCode", Matchers.is("brandCode1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.shipCode", Matchers.is("shipCode1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cviPortEntries[0].voyageTypeArrival", Matchers.is("voyageTypeArrival0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cviPortEntries[0].port.id", Matchers.is("portId0")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cviPortEntries[0].port.name", Matchers.is("portName0")));
//                .andExpect(MockMvcResultMatchers.jsonPath("$.cviPortEntries[0].apisSubmissions[0].id", Matchers.is("submissionId0")))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.cviPortEntries[0].apisSubmissions[0].ruleName", Matchers.is("ruleName0")));
    }

    @Test
    public void listAllCvi() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void insertCvi() throws Exception {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto = MockDataProvider.createCviDto(cviDtoNumber);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test(expected = CviAlreadyExistsException.class)
    public void insertCvi_startDate1startDate2endDate1endDate2() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getStartDate() - 100);
        cviDto2.setEndDate(cviDto1.getStartDate() + 100);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        try {
            mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(cviDto2))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());

        } catch (NestedServletException e) {
            e.printStackTrace();
            throw e.getCause();
        }
    }

    @Test(expected = CviAlreadyExistsException.class)
    public void insertCvi_startDate2startDate1endDate1endDate2() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getStartDate() + 100);
        cviDto2.setEndDate(cviDto1.getEndDate() + 100);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        try {
            mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(cviDto2))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());

        } catch (NestedServletException e) {
            e.printStackTrace();
            throw e.getCause();
        }
    }

    @Test(expected = CviAlreadyExistsException.class)
    public void insertCvi_startDate1startDate2endDate2endDate1() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getStartDate() + 100);
        cviDto2.setEndDate(cviDto1.getEndDate() - 100);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        try {
            mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(cviDto2))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());

        } catch (NestedServletException e) {
            e.printStackTrace();
            throw e.getCause();
        }
    }

    @Test(expected = CviAlreadyExistsException.class)
    public void insertCvi_startDate2startDate1endDate2endDate1() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getStartDate() - 100);
        cviDto2.setEndDate(cviDto1.getEndDate() - 100);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        try {
            mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(cviDto2))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());

        } catch (NestedServletException e) {
            e.printStackTrace();
            throw e.getCause();
        }
    }

    @Test
    public void insertCvi_startDate1endDate1startDate2endDate2Ok() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getStartDate() - 200);
        cviDto2.setEndDate(cviDto1.getStartDate() - 100);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto2))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void insertCvi_startDate2endDate2startDate1endDate1Ok() throws Throwable {
        int cviDtoNumber = MOCK_COUNT + 1;
        CruiseVoyageItineraryDto cviDto1 = MockDataProvider.createCviDto(cviDtoNumber);
        CruiseVoyageItineraryDto cviDto2 = MockDataProvider.createCviDto(cviDtoNumber + 1);
        cviDto2.setShipId(cviDto1.getShipId());
        cviDto2.setStartDate(cviDto1.getEndDate() + 200);
        cviDto2.setEndDate(cviDto1.getEndDate() + 300);

        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto1))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cviDto2))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void updateCvi() throws Exception {
        int old = MOCK_COUNT - 1;
        String updated = "NEW_V";
        CruiseVoyageItineraryDto cviDto = cviCouchbaseRepository.findOneFull(getCviId(old));
        cviDto.setBrandCode(updated);
        cviDto.getCviPortEntries().get(0).setVoyageTypeArrival(updated);

        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                .content(json(cviDto))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(
                cviCouchbaseRepository.findOneFull(getCviId(old)).getBrandCode(),
                Matchers.is(updated));
        MatcherAssert.assertThat(
                cviCouchbaseRepository.findOneFull(getCviId(old)).getCviPortEntries().get(0).getVoyageTypeArrival(),
                Matchers.is(updated));
    }

    @Test
    public void deleteCvi() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getCviId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(Iterables.size(cviCouchbaseRepository.findAll()), Matchers.is(2));
    }

    @Test
    public void paginationAndSortWorks() throws Exception {
        String path = "/page?page=0&size=2&sort=shipCode,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].shipCode", Matchers.is("shipCode2")));
    }

    @Test
    public void sortWorks() throws Exception {
        String path = "?sort=shipCode,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].shipCode", Matchers.is("shipCode2")));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        String shipCode = "shipCode0";
        String path = "/?sort=shipCode,DESC&shipCode=" + shipCode;
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].shipCode", Matchers.equalTo(shipCode)));
    }

    @Test
    public void searchByPortId() throws Exception {
        CruiseVoyageItineraryDto cruiseVoyageItineraryDto = createCviDto(10);
        cruiseVoyageItineraryDto.setCviPortEntries(ImmutableList.of(createCviPortEntryDto(10), createCviPortEntryDto(11)));
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(cruiseVoyageItineraryDto))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());

        String portId = getPortId(10);
        String path = "/?portId=" + portId;
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].id", Matchers.equalTo(getCviId(10))));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        String expectedShipCode = "NEW_CODE";

        CruiseVoyageItineraryDto cviDto = cviCouchbaseRepository.findOneFull(getCviId(0));
        cviDto.setShipCode(expectedShipCode);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, cviDto.getId())
                .content(json(cviDto))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + cviDto.getId()))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.shipCode", Matchers.is(expectedShipCode)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }
}
