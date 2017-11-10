package com.trident.apis.entitymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipIdentity;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ShipDtoForLimitedQuery extends CouchbaseEntityWithId {

    private ShipIdentity shipIdentity;
    private String brandCode;
    private String shipTridentCode;

    public ShipDtoForLimitedQuery() {
        super(ShipEntity.SHIP);
    }

    @JsonIgnore
    @Override
    public String getName() {
        return shipIdentity.getName();
    }
}
