package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

/**
 * Created by sergii on 11/10/16.
 */
public class CountryNameLookup extends LookupObject<String, String> {

    public static final String COUNTRY_NAME_LOOKUP = "countryNameLookup";

    public CountryNameLookup() {
        super(COUNTRY_NAME_LOOKUP);
    }

    public CountryNameLookup(String field, String objectId) {
        super(field, objectId, COUNTRY_NAME_LOOKUP);
    }

}