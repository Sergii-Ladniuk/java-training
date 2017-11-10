package com.trident.apis.entitymanager.model;

import com.couchbase.client.java.repository.annotation.Field;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.couchbase.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Document
public class Country extends CouchbaseEntityWithId {

    public static final String COUNTRY = "country";
    @Field
    @NotNull
    private String name;
    @Field
    @NotNull
    /**
     *
     * CountryDto ISO 3166-1 2 Letter Code
     */
    private String code;
    /**
     * CountryDto ISO 3166-1 3 Letter Code
     */
    @Field
    @NotNull
    private String code3;
    @Field
    private String formalName;

    public Country() {
        super(COUNTRY);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * CountryDto ISO 3166-1 2 Letter Code getter
     */
    public String getCode() {
        return code;
    }

    /**
     * CountryDto ISO 3166-1 2 Letter Code setter
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * CountryDto ISO 3166-1 3 Letter Code getter
     */
    public String getCode3() {
        return code3;
    }

    /**
     * CountryDto ISO 3166-1 3 Letter Code setter
     */
    public void setCode3(String code3) {
        this.code3 = code3;
    }

    public String getFormalName() {
        return formalName;
    }

    public void setFormalName(String formalName) {
        this.formalName = formalName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
