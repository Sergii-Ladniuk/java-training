package com.trident.apis.entitymanager.model;

import com.couchbase.client.deps.com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.dto.apis.knowledge.ship.Ship;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShipEntity extends CouchbaseEntityWithId {

    public static final String SHIP = "ship";

    public ShipEntity() {
        super(SHIP);
    }

    public static ShipEntity of(Ship ship) {
        ShipEntity shipEntity = new ShipEntity();
        shipEntity.setShip(ship);
        return shipEntity;
    }

    public static ShipEntity of(String id, Ship ship) {
        ShipEntity shipEntity =  ShipEntity.of(ship);
        shipEntity.setId(id);
        return shipEntity;
    }

    private Ship ship;

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return ship.getShipIdentity().getName();
    }
}
