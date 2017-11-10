package com.trident.apis.entitymanager.service;

import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ShipMapper {

    private Mapper mapper;

    public ShipDto toShipEx(ShipEntity shipEntity) {
        return mapper.map(shipEntity, ShipDto.class);
    }

    public ShipEntity fromShipEx(ShipDto dto) {
        return mapper.map(dto, ShipEntity.class);
    }

    @Lazy
    @Autowired
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }
}
