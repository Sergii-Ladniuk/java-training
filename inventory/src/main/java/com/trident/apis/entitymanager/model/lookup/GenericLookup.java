package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

/**
 * Created by sergii on 11/10/16.
 */
public class GenericLookup extends LookupObject<String, String> {

    public GenericLookup() {
        super("genericLookup");
    }

    public GenericLookup(String field, String objectId) {
        super(field, objectId, "genericLookup");
    }

}
