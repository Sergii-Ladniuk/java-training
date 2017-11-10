package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

/**
 * Created by sergii on 11/10/16.
 */
public class PortNameLookup extends LookupObject<String, String> {

    public static final String DOCUMENT_NAME = "portNameLookup";

    public PortNameLookup() {
        super(DOCUMENT_NAME);
    }

    public PortNameLookup(String field, String objectId) {
        super(field, objectId, DOCUMENT_NAME);
    }

}
