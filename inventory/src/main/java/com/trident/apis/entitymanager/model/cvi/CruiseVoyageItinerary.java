package com.trident.apis.entitymanager.model.cvi;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.dto.apis.cvi.CviPortEntry;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author jacoblloyd
 */
@Getter
@Setter
public class CruiseVoyageItinerary extends CouchbaseEntityWithId {

    public static final String CRUISE_VOYAGE_ITINERARY = "CruiseVoyageItinerary";

    @NotNull
    private String brandCode;
    @NotNull
    private String shipCode;
    @NotNull
    private String voyageNumber;
    @NotNull
    private String description;
    @NotNull
    private String notes;
    @NotNull
    private long startDate;
    @NotNull
    private long endDate;
    private long dateModified;
    @NotNull
    @Min(1)
    private List<CviPortEntry> cviPortEntries;
    @NotNull
    private String shipId;

    public CruiseVoyageItinerary() {
        super(CRUISE_VOYAGE_ITINERARY);
    }

    @JsonIgnore
    @Override
    public String getName() {
        return description;
    }
}
