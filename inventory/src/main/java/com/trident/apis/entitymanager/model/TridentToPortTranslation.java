package com.trident.apis.entitymanager.model;

import com.couchbase.client.java.repository.annotation.Field;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trident.shared.immigration.dto.apis.translation.TranslationRule;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortFieldTranslation;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import org.springframework.data.couchbase.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Document
@JsonPropertyOrder(alphabetic=true)
public class TridentToPortTranslation extends CouchbaseEntityWithId {

    public static final String TYPE = "tridentToPortTranslation";

    @Field
    @NotNull
    private String name;

    @Field
    @NotNull
    @JsonPropertyOrder(alphabetic=true)
    private Map<String, TridentToPortFieldTranslation> fieldsTranslation;

    private List<TranslationRule> translationRules;

    public TridentToPortTranslation() {
        super(TYPE);
    }

    public String getName() {
        return name;
    }

    public TridentToPortTranslation setName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, TridentToPortFieldTranslation> getFieldsTranslation() {
        return fieldsTranslation;
    }

    public void setFieldsTranslation(Map<String, TridentToPortFieldTranslation> fieldsTranslation) {
        this.fieldsTranslation = fieldsTranslation;
    }

    public List<TranslationRule> getTranslationRules() {
        return translationRules;
    }

    public void setTranslationRules(List<TranslationRule> translationRules) {
        this.translationRules = translationRules;
    }
}
