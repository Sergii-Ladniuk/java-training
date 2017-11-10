package com.trident.apis.entitymanager.model;

import com.couchbase.client.java.repository.annotation.Field;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.data.couchbase.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Document
@Getter @Setter
public class Port extends CouchbaseEntityWithId {

    public static final String PORT = "port";

    @Field
    @NotNull
    private String name;
    @Field
    @NotNull
    private String countryId;
    @Field
    private String state;
    @Field
    @NotNull
    private String code; // TODO code, unCode, polarCode?
    @Field
    private String description;
    @Field
    @NotNull
    private String unCode;
    @Field
    @NotNull
    private String polarCode;

    private String notes;
    private String noteId;
    private String noteFileName;
    private Boolean richFormatNotes;

    public Port() {
        super(PORT);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
