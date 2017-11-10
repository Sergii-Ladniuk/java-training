package com.trident.apis.entitymanager.model.lookup;

import com.trident.shared.immigration.model.LookupObject;

public class ApisRuleNameLookup extends LookupObject<String,String> {

    public static final String APIS_RULE_NAME_LOOKUP = "apisRuleNameLookup";

    public ApisRuleNameLookup() {
        super(APIS_RULE_NAME_LOOKUP);
    }

    public ApisRuleNameLookup(String field, String objectId) {
        super(field, objectId, APIS_RULE_NAME_LOOKUP);
    }

}
