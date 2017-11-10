package com.trident.apis.entitymanager.controller;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.config.AppConfiguration;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.apis.entitymanager.repository.ShipCouchbaseRepository;
import com.trident.apis.entitymanager.service.ShipMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AppConfiguration.class)
@Category(CouchbaseIntegrationTest.class)
public class ShipEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/ships";

    @Autowired
    private ShipCouchbaseRepository shipRepositoryDecorator;
    @Autowired
    private ShipMapper shipMapper;

    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.CVI);
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COMPANY_ENTITY);
        mockDataProvider.insertMock(MockDataProvider.MockType.SHIP_ENTITY, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.COUNTRY, MOCK_COUNT + 2);
        mockDataProvider.insertMock(MockDataProvider.MockType.COMPANY_ENTITY, MOCK_COUNT + 2);
        mockDataProvider.insertMock(MockDataProvider.MockType.CVI, MOCK_COUNT);

        Iterable<ShipEntity> ships = shipRepositoryDecorator.findAll();
        assert ships != null;
        assert Iterators.size(ships.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COUNTRY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COMPANY_ENTITY);
    }

    @Test
    public void listAllShips() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void shipById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getShipId(1)))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(getShipId(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipIdentity.name", Matchers.is("ship name1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipTechnicalSpec.shipBuildInfo.builtAtCountry.id", Matchers.is("countryId1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipTechnicalSpec.shipBuildInfo.manufacturer.id", Matchers.is("companyId1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipLegal.owner.id", Matchers.is("companyId1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipLegal.agent.id", Matchers.is("companyId1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ship.shipLegal.nationalityCountry.id", Matchers.is("countryId1")));
    }

    @Test
    public void insertShip() throws Exception {
        int shipNumber = MOCK_COUNT + 1;
        ShipEntity ship = createShipEntity(shipNumber);
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(ship))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(
                shipRepositoryDecorator.findOneFull(getShipId(shipNumber)).getShip().getShipIdentity().getName(),
                Matchers.is(getShipName(shipNumber)));
    }

    @Test
    public void updateShip() throws Exception {
        int old = MOCK_COUNT - 1;
        int updated = MOCK_COUNT + 2;
        ShipEntity ship = shipRepositoryDecorator.findOne(getShipId(old));
        ship.getShip().getShipIdentity().setName(getShipName(updated));
        String json = json(ship);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH)
                .content(json)
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(
                shipRepositoryDecorator.findOne(getShipId(old)).getShip().getShipIdentity().getName(),
                Matchers.is(getShipName(updated)));
    }

    @Test
    public void deleteShip() throws Exception {
        mvc.perform(delete("/api/v1/apis/cvi/{cviId}", getCviId(2)));
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getShipId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(Iterables.size(shipRepositoryDecorator.findAll()), Matchers.is(2));
    }

    @Test
    public void deleteShipWithException() throws Exception{
        try {
            mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getShipId(1)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
        }
    }

    @Test
    public void paginationAndSortWorks() throws Exception {
        String path = "/page?page=0&size=2&sort=ship.shipIdentity.name,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].ship.shipIdentity.name", Matchers.is("ship name2")));
    }

    @Test
    public void sortWorks() throws Exception {
        String path = "/?sort=ship.shipIdentity.name,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].ship.shipIdentity.name", Matchers.is("ship name2")));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        final String expectedName = "Some name";
        ShipEntity shipEntity = createShipEntity(MOCK_COUNT + 2);
        shipEntity.getShip().getShipIdentity().setName(expectedName);
        shipRepositoryDecorator.save(shipEntity, "someUser");

        String path = "?sort=ship.shipIdentity.name,DESC&name=ome name";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].ship.shipIdentity.name", Matchers.equalTo(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        ShipEntity modifiedShipEntity = shipRepositoryDecorator.findOne(getShipId(1));
        String expectedName = "new name";
        modifiedShipEntity.getShip().getShipIdentity().setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedShipEntity.getId())
                .content(json(modifiedShipEntity))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedShipEntity.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.ship.shipIdentity.name", Matchers.is(expectedName)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }
}