package com.trident.apis.entitymanager.config;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.trident.apis.entitymanager.config.couchbase.ApisCouchbaseConverter;
import com.trident.shared.immigration.config.AppCouchbaseProperties;
import com.trident.shared.immigration.repository.BucketWithRetries;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCouchbaseRepositories
public class CouchbaseConfiguration extends AbstractCouchbaseConfiguration {

    @Autowired
    private AppCouchbaseProperties couchbaseProperties;

    @Override
    protected List<String> getBootstrapHosts() {
        return Arrays.asList(couchbaseProperties.getHost().split(","));
    }

    @Override
    protected String getBucketName() {
        return couchbaseProperties.getBucket();
    }

    @Override
    protected String getBucketPassword() {
        return couchbaseProperties.getPassword();
    }

    @Override
    protected CouchbaseEnvironment getEnvironment() {
        return DefaultCouchbaseEnvironment.builder()
                .connectTimeout(TimeUnit.SECONDS.toMillis(30))
                .computationPoolSize(6)
                .build();
    }

    @Override
    public MappingCouchbaseConverter mappingCouchbaseConverter() throws Exception {
        ApisCouchbaseConverter apisCouchbaseConverter = new ApisCouchbaseConverter(couchbaseMappingContext(), typeKey());
        apisCouchbaseConverter.setCustomConversions(customConversions());
        return apisCouchbaseConverter;
    }

    @Override
    public String typeKey() {
        return RepositoryDecorator.TYPE_KEY;
    }

    @Bean("bucketWithRetry")
    public Bucket bucketWithRetry(){
        return new BucketWithRetries();
    }
}
