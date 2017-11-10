package com.trident.apis.entitymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;

import java.util.Set;

public class CodeDictionary extends CouchbaseEntityWithId {

    public static final String CODE_DICTIONARY = "codeDictionary";

    private Set<String> codes;

    public CodeDictionary() {
        super(CODE_DICTIONARY);
    }

    public Set<String> getCodes() {
        return codes;
    }

    public void setCodes(Set<String> codes) {
        this.codes = codes;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return getId();
    }
}
