package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

public class ApisTemplateNameLookup extends LookupObject<String, String> {

    public static final String APIS_TEMPLATE_NAME_LOOKUP = "apisTemplateNameLookup";

    public ApisTemplateNameLookup() {
        super(APIS_TEMPLATE_NAME_LOOKUP);
    }

    public ApisTemplateNameLookup(String field, String objectId) {
        super(field, objectId, APIS_TEMPLATE_NAME_LOOKUP);
    }
}
