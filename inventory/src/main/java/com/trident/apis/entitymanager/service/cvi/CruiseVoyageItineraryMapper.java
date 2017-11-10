package com.trident.apis.entitymanager.service.cvi;

import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CruiseVoyageItineraryMapper {
    private Mapper mapper;

    public CruiseVoyageItineraryDto toCviDto(CruiseVoyageItinerary cvi) {
        return mapper.map(cvi, CruiseVoyageItineraryDto.class);
    }

    public CruiseVoyageItinerary fromCviDto(CruiseVoyageItineraryDto cviDto) {
        return mapper.map(cviDto, CruiseVoyageItinerary.class);
    }

    @Lazy
    @Autowired
    public void setMapper(Mapper mapper){
        this.mapper = mapper;
    }
}
