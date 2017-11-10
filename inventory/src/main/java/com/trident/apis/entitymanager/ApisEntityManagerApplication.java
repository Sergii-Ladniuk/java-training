package com.trident.apis.entitymanager;

import com.trident.apis.entitymanager.config.AppConfiguration;
import com.trident.apis.entitymanager.config.CouchbaseConfiguration;
import com.trident.shared.immigration.api.client.config.ApiClientConfiguration;
import com.trident.shared.immigration.config.DefaultImmigrationConfiguration;
import com.trident.shared.immigration.config.SwaggerConfig;
import com.trident.shared.immigration.history.HistoryService;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Import({CouchbaseConfiguration.class, AppConfiguration.class, SwaggerConfig.class, DefaultImmigrationConfiguration.class, ApiClientConfiguration.class})
@SpringBootApplication
@EnableWebMvc
@ComponentScan(basePackageClasses = {RepositoryDecorator.class, ApisEntityManagerApplication.class, HistoryService.class})
public class ApisEntityManagerApplication
{

    public static void main( String[] args ) {
        SpringApplication.run(ApisEntityManagerApplication.class, args);
    }

}
