package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

/**
 * Created by sergii on 11/10/16.
 */
public class CountryCodeLookup extends LookupObject<String, String> {

    public static final String COUNTRY_CODE_LOOKUP = "countryCodeLookup";

    public CountryCodeLookup() {
        super(COUNTRY_CODE_LOOKUP);
    }

    public CountryCodeLookup(String field, String objectId) {
        super(field, objectId, COUNTRY_CODE_LOOKUP);
    }

}
