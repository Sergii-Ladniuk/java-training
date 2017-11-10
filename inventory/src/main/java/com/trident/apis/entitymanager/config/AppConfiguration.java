package com.trident.apis.entitymanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.service.CertificateListDozerConverter;
import com.trident.apis.entitymanager.service.CompanyDozerConverter;
import com.trident.apis.entitymanager.service.CountryDozerConverter;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@PropertySource("classpath:application.yaml")
@EnableWebMvc
@Import({RepositoryConfig.class})
public class AppConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("POST", "GET", "PUT", "DELETE", "OPTIONS");
            }
        };
    }

    @Bean
    public MappingJackson2HttpMessageConverter jackson2Converter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter("filter__", SimpleBeanPropertyFilter.serializeAllExcept()));
        return objectMapper;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        super.addResourceHandlers(registry);
        registry
                .addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry
                .addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public CountryDozerConverter countryDozerConverter() {
        return new CountryDozerConverter();
    }

    @Bean
    public CompanyDozerConverter companyDozerConverter() {
        return new CompanyDozerConverter();
    }

    @Bean
    public CertificateListDozerConverter certificateListDozerConverter() {
        return new CertificateListDozerConverter();
    }

    @Bean
    public Mapper mapper() {
        DozerBeanMapper mapper = new DozerBeanMapper(ImmutableList.of("mappings/ship-mapping.xml",
                                                                        "mappings/cruiseVoyageItinerary-mapping.xml"));
        mapper.setCustomConverters(ImmutableList.of(countryDozerConverter(),
                                                    companyDozerConverter(),
                                                    certificateListDozerConverter()));
        return mapper;
    }
}
