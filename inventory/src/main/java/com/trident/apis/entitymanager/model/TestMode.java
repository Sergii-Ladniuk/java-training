package com.trident.apis.entitymanager.model;

import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Getter;

@Getter
public class TestMode extends CouchbaseEntityWithId {

    public static final String DIVE_TEST_MODE = "dive_TestMode";
    private Boolean enabled;

    public TestMode() {
        super(DIVE_TEST_MODE);
    }

    public TestMode(Boolean enabled) {
        super(DIVE_TEST_MODE);
        this.enabled = enabled;
    }

    @Override
    public String getId() {
        return DIVE_TEST_MODE;
    }

    @Override
    public String getName() {
        return "Test Mode";
    }
}
