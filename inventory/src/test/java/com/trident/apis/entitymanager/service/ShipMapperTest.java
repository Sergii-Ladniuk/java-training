package com.trident.apis.entitymanager.service;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.ShipDtoForLimitedQuery;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.shared.immigration.dto.apis.knowledge.legal.CompanyDto;
import com.trident.shared.immigration.dto.apis.knowledge.ship.*;
import com.trident.shared.immigration.dto.apis.port.CountryDto;
import org.dozer.DozerBeanMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ShipMapperTest {

    public static final String SOME_BRAND = "some brand";
    public static final String SOME_NAME = "some name";
    public static final String SOME_ID = "some id";
    public static final String NATIONALITY_COUNTRY_ID = "nationality country id";
    public static final CountryDto NAT_COUNTRY_DTO = new CountryDto();
    public static final CountryDto BUILD_COUNTRY_DTO = new CountryDto();
    public static final String BUILD_COUNTRY_ID = "build country id";
    public static final String OWNER_ID = "owner id";
    public static final CompanyDto OWNER_COMPANY = new CompanyDto();
    @InjectMocks
    private ShipMapper shipMapper;
    @Mock
    private CountryMapper countryMapper;
    @Mock
    private CompanyMapper companyMapper;
    @InjectMocks
    private CountryDozerConverter countryDozerConverter;
    @InjectMocks
    private CompanyDozerConverter companyDozerConverter;

    @Before
    public void setUp() throws Exception {
        DozerBeanMapper mapper = new DozerBeanMapper(ImmutableList.of("/mappings/ship-mapping.xml"));
        mapper.setCustomConverters(ImmutableList.of(countryDozerConverter, companyDozerConverter));
        shipMapper.setMapper(mapper);
    }
    
    @Test
    public void testLimitedQueryMap() throws Exception {
        DozerBeanMapper mapper = new DozerBeanMapper(ImmutableList.of("mappings/ship-mapping.xml"));
        ShipDtoForLimitedQuery source = new ShipDtoForLimitedQuery();
        source.setBrandCode("brand");
        source.setShipTridentCode("tc");
        source.setShipIdentity(ShipIdentity.builder()
                .name("name")
                .build());
        ShipEntity result = mapper.map(source, ShipEntity.class);
        assertThat(result.getShip().getBrandCode(), Matchers.is("brand"));
        assertThat(result.getShip().getShipIdentity().getName(), Matchers.is("name"));
        assertThat(result.getShip().getShipTridentCode(), Matchers.is("tc"));
    }

    private ShipEntity getShipEntity() {
        return ShipEntity.of(
                SOME_ID,
                Ship.builder()
                        .brandCode(SOME_BRAND)
                        .shipIdentity(ShipIdentity.builder()
                                .name(SOME_NAME)
                                .build())
                        .shipLegal(ShipLegal.builder()
                                .nationalityCountryId(NATIONALITY_COUNTRY_ID)
                                .shipCertificateList(ShipCertificateList.of(ImmutableList.of(
                                        ShipCertificate.builder()
                                                .name("certificate")
                                                .build()
                                )))
                                .ownerId(OWNER_ID)
                                .build())
                        .shipTechnicalSpec(ShipTechnicalSpec.builder()
                                .shipBuildInfo(ShipBuildInfo.builder()
                                        .builtAtCountryId(BUILD_COUNTRY_ID)
                                        .manufacturerId(OWNER_ID)
                                        .build())
                                .build())
                        .build());
    }

}