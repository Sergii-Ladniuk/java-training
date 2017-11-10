package com.trident.apis.entitymanager.repository;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trident.apis.entitymanager.filter.TestModeFilter;
import com.trident.apis.entitymanager.model.TestMode;
import com.trident.shared.immigration.exception.PersistenceException;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Repository
public class TestModeRepository {

    private static final Logger logger = Logger.getLogger(TestModeRepository.class);

    @Autowired
    @Qualifier("bucketWithRetry")
    @Setter
    private Bucket bucket;

    public TestMode load() {
        Boolean enabled = isEnabled();
        return new TestMode(enabled);
    }

    public Boolean isEnabled() {
        JsonDocument jsonDocument = bucket.get(TestMode.DIVE_TEST_MODE);
        if (jsonDocument == null) {
            return false;
        } else {
            return jsonDocument.content().getBoolean("enabled");
        }
    }

    public void save(TestMode testMode) {
        save(testMode.getEnabled());
    }

    public void save(Boolean enabled) {
        String id = TestMode.DIVE_TEST_MODE;
        JsonObject data =
                JsonObject.create()
                        .put("enabled", enabled);
        bucket.upsert(JsonDocument.create(id, data));
        logger.info("Cleaning up database...");
        bucket.query(N1qlQuery.simple(
                "DELETE FROM " + bucket.name() + " WHERE __type like 'test_dive_%'"));
    }
}
