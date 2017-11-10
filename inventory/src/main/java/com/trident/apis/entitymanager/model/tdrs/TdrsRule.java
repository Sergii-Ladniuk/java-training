package com.trident.apis.entitymanager.model.tdrs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trident.shared.immigration.dto.tdrs.TdrsRuleDto;
import com.trident.shared.immigration.dto.tdrs.recommendation.RecommendationNode;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.trident.apis.entitymanager.model.ModelConstants.LOOKUP;
import static com.trident.apis.entitymanager.model.ModelConstants.NAME;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TdrsRule extends CouchbaseEntityWithId {

    public static final String TDRS_RULE = "tdrsRule";
    public static final String TDRS_RULE_NAME_LOOKUP = TDRS_RULE + NAME + LOOKUP;

    private String name;
    private String condition;
    private String[] citizenshipCountryIds;
    private String destinationCountryId;
    private RecommendationNode recommendation;
    private Integer ordinal;
    private Set<String> documentIds;


    public TdrsRule() {
        super(TDRS_RULE);
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal) {
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String[] getCitizenshipCountryIds() {
        return citizenshipCountryIds;
    }

    public void setCitizenshipCountryIds(String[] citizenshipCountryIds) {
        this.citizenshipCountryIds = citizenshipCountryIds;
    }

    public String getDestinationCountryId() {
        return destinationCountryId;
    }

    public void setDestinationCountryId(String destinationCountryId) {
        this.destinationCountryId = destinationCountryId;
    }

    public RecommendationNode getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(RecommendationNode recommendation) {
        this.recommendation = recommendation;
    }

    public Set<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(Set<String> documentIds) {
        this.documentIds = documentIds;
    }

    public TdrsRuleDto toDto() {
        TdrsRuleDto dto = new TdrsRuleDto();
        dto.setId(getId());
        dto.set__type(get__type());
        dto.set__etag(get__etag());
        dto.setName(getName());
        dto.setCondition(getCondition());
        // citizenship and destination countries are not set as it requires DB call
        dto.setRecommendation(getRecommendation());
        dto.setOrdinal(getOrdinal());
        return dto;
    }

    public static TdrsRule fromDto(TdrsRuleDto dto) {
        if (dto == null) { return null; }
        TdrsRule rule = new TdrsRule();
        rule.setId(dto.getId());
        rule.set__etag(dto.get__etag());
        rule.setName(dto.getName());
        rule.setCondition(dto.getCondition());
        rule.setCitizenshipCountryIds(dto.getCitizenshipCountries() != null ?
                Stream.of(dto.getCitizenshipCountries()).map(c -> c.getId()).collect(Collectors.toList()).toArray(new String[0]) :
                null);
        rule.setDestinationCountryId(dto.getDestinationCountry() != null ? dto.getDestinationCountry().getId() : null);
        rule.setRecommendation(dto.getRecommendation());
        rule.setOrdinal(dto.getOrdinal());
        return rule;
    }
}
