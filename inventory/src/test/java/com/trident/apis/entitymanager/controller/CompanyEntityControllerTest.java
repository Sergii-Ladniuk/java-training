package com.trident.apis.entitymanager.controller;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.trident.apis.entitymanager.ApisEntityManagerApplication;
import com.trident.apis.entitymanager.CouchbaseIntegrationTest;
import com.trident.apis.entitymanager.MockDataProvider;
import com.trident.apis.entitymanager.config.AppConfiguration;
import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.CompanyEntity;
import com.trident.apis.entitymanager.repository.CompanyCouchbaseRepository;
import com.trident.apis.entitymanager.repository.ShipCouchbaseRepository;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.dto.apis.knowledge.legal.Address;
import com.trident.shared.immigration.dto.apis.knowledge.legal.Company;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.util.RestUtils;
import com.trident.test.MockMvcTestBase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import java.util.List;
import java.util.stream.Collectors;

import static com.trident.apis.entitymanager.MockDataProvider.getCompanyId;
import static com.trident.apis.entitymanager.MockDataProvider.getCviId;
import static com.trident.apis.entitymanager.MockDataProvider.getShipId;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import(AppConfiguration.class)
@Category(CouchbaseIntegrationTest.class)
public class CompanyEntityControllerTest extends MockMvcTestBase {

    private static final int MOCK_COUNT = 3;
    private static final String BASE_PATH = "/api/v1/apis/companies";

    @Autowired
    private CompanyCouchbaseRepository companyCouchbaseRepository;

    @Autowired
    private MockDataProvider mockDataProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COMPANY_ENTITY);
        mockDataProvider.insertMock(MockDataProvider.MockType.SHIP_ENTITY, MOCK_COUNT);
        mockDataProvider.insertMock(MockDataProvider.MockType.COMPANY_ENTITY, MOCK_COUNT);
        Iterable<CompanyEntity> companies = companyCouchbaseRepository.findAll();
        assert companies != null;
        assert Iterators.size(companies.iterator()) == MOCK_COUNT;
    }

    @After
    public void tearDown() throws Exception {
        mockDataProvider.cleanDb(MockDataProvider.MockType.SHIP_ENTITY);
        mockDataProvider.cleanDb(MockDataProvider.MockType.COMPANY_ENTITY);

    }

    @Test
    public void listAllCompanys() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$", IsCollectionWithSize.hasSize(MOCK_COUNT)));
    }

    @Test
    public void companyById() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(BASE_PATH + "/" + getCompanyId(1)))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(getCompanyId(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.company.name", Matchers.is("company name1")));
    }

    @Test
    public void insertCompany() throws Exception {
        int companyNumber = MOCK_COUNT + 1;
        CompanyEntity company = new CompanyEntity();
        company.setId(getCompanyId(companyNumber));
        company.setCompany(new Company());
        company.getCompany().setName("companyName" + companyNumber);
        company.getCompany().setAddress(new Address("s","sss","ssss","sssss","ssssss"));
        company.getCompany().setBrandCode("some brand");
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                    .content(json(company))
                    .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Status: " + mvcResult.getResponse().getStatus() + ". Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(companyCouchbaseRepository.findOne(getCompanyId(companyNumber)).getCompany().getName(), Matchers.is("companyName" + companyNumber));
    }

    @Test
    public void updateCompany() throws Exception {
        int companyNumberToUpdate = MOCK_COUNT + 1;
        int companyNumberToInsert = MOCK_COUNT + 2;
        CompanyEntity company = new CompanyEntity();
        company.setId(getCompanyId(companyNumberToUpdate));
        company.setCompany(new Company());
        company.getCompany().setName("companyName" + companyNumberToInsert);
        company.getCompany().setAddress(new Address("s","sss","ssss","sssss","ssssss"));
        company.getCompany().setBrandCode("some brand");
        mvc.perform(MockMvcRequestBuilders.post(BASE_PATH)
                .content(json(company))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println("Response: " + mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(
                companyCouchbaseRepository.findOne(getCompanyId(companyNumberToUpdate)).getCompany().getName(),
                Matchers.is("companyName" + companyNumberToInsert));
    }

    @Test
    public void  deleteCompany() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/apis/cvi/" + getCviId(2)));
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1/apis/ships/" + getShipId(2)));
        mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getCompanyId(2)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        MatcherAssert.assertThat(Iterables.size(companyCouchbaseRepository.findAll()), Matchers.is(2));
    }


    @Test
    public void deleteCompanyWithShip() throws Exception{
        try {
            mvc.perform(MockMvcRequestBuilders.delete(BASE_PATH + "/" + getCompanyId(2)));
        } catch (NestedServletException ex) {
            assert (ex.getCause() instanceof ForeignConstraintViolationException);
            assert (ex.getMessage().contains("Ship"));
        }
    }

    @Test
    public void testOptimisticLockingWorks() throws Exception {
        String apisCompanyString = mvc.perform(get(BASE_PATH + "/" + getCompanyId(1)))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        CompanyEntity companyEntity = objectMapper.readValue(apisCompanyString, CompanyEntity.class);
        CompanyEntity apisCompanyStale = objectMapper.readValue(apisCompanyString, CompanyEntity.class);
        companyEntity.getCompany().setName("CompanyEntity new value");
//        companyEntity.setType(DocumentOutputType.EXCEL);
        mvc.perform(
                put(BASE_PATH)
                        .content(json(companyEntity))
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(status().isOk());
        apisCompanyStale.getCompany().setName("CompanyEntity stale value");
        try {
            mvc.perform(
                    put(BASE_PATH)
                            .content(json(companyEntity))
                            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                    // expect conflict status
                    .andExpect(status().is4xxClientError())
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Ignore("Now no constraints defined for company")
    @Test
    public void itShouldNotSaveCompanyWithExistingNameOnUpdate() throws Exception {
        CompanyEntity companyEntity = companyCouchbaseRepository.findOne(getCompanyId(1));
        companyEntity.getCompany().setName("companyName0");
        try {
            mvc.perform(put(BASE_PATH)
                    .content(json(companyEntity))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Ignore("Now no constraints defined for company")
    @Test
    public void itShouldNotSaveCompanyWithExistingNameOnInsert() throws Exception {
        CompanyEntity companyEntity = new CompanyEntity();
        companyEntity.setCompany(new Company());
        companyEntity.getCompany().setName("companyName0");
        companyEntity.getCompany().setAddress(new Address("s","sss","ssss","sssss","ssssss"));
        companyEntity.getCompany().setBrandCode("some brand");
        try {
            mvc.perform(post(BASE_PATH)
                    .content(json(companyEntity))
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                    .andExpect(status().is(409));
        } catch (NestedServletException | OptimisticLockingFailureException e) {
            // ignore, due to spring mock mvc issue
        }
    }

    @Test
    public void paginationAndSortWorks() throws Exception {
        String path = "/page?page=0&size=2&sort=company.name,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.totalElements", Matchers.is(MOCK_COUNT)))
                .andExpect(jsonPath("$.totalPages", Matchers.is(2)))
                .andExpect(jsonPath("$.size", Matchers.is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].company.name", Matchers.is("company name2")));
    }

    @Test
    public void sortWorks() throws Exception {
        String path = "/?sort=company.name,DESC";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].company.name", Matchers.is("company name2")));
    }

    @Test
    public void searchAndSortShouldWork() throws Exception {
        final String expectedName = "Some name";
        CompanyEntity companyEntity = new CompanyEntity();
        companyEntity.setId(getCompanyId(5));
        companyEntity.setCompany(new Company());
        companyEntity.getCompany().setName(expectedName);
        companyEntity.getCompany().setAddress(new Address("s","sss","ssss","sssss","ssssss"));
        companyEntity.getCompany().setBrandCode("some brand");
        companyCouchbaseRepository.save(companyEntity, "someUser");

        String path = "?sort=company.name,DESC&name=ome name";
        mvc.perform(get(BASE_PATH + path))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.[0].company.name", Matchers.equalTo(expectedName)));
    }

    @Test
    public void checkHistory() throws Exception {
        String expectedUser = "historyUser";
        CompanyEntity modifiedCompanyEntity = companyCouchbaseRepository.findOne(getCompanyId(1));
        String expectedName = "new name";
        modifiedCompanyEntity.getCompany().setName(expectedName);
        mvc.perform(MockMvcRequestBuilders.put(BASE_PATH, modifiedCompanyEntity.getId())
                .content(json(modifiedCompanyEntity))
                .header("content-type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header(RestUtils.HEADER_USER_ID, expectedUser))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mvc.perform(get(BASE_PATH + "/history/" + modifiedCompanyEntity.getId()))
                .andDo(mvcResult -> System.out.println(mvcResult.getResponse().getContentAsString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.[0].entity.company.name", Matchers.is(expectedName)))
                .andExpect(jsonPath("$.[0].userId", Matchers.is(expectedUser)));
    }
}