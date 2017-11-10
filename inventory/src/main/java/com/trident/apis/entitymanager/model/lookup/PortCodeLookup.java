package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

/**
 * Created by sergii on 11/10/16.
 */
public class PortCodeLookup extends LookupObject<String, String> {

    public static final String DOCUMENT_NAME = "portCodeLookup";

    public PortCodeLookup() {
        super(DOCUMENT_NAME);
    }

    public PortCodeLookup(String field, String objectId) {
        super(field, objectId, DOCUMENT_NAME);
    }

}
