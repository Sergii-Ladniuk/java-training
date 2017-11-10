package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.CompanyEntity;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vdmitrovskiy on 7/25/17.
 */

public class CompanyCouchbaseRepository extends RepositoryDecorator<CompanyEntity> {

    private ShipCouchbaseRepository shipCouchbaseRepository;
    @Autowired
    public void setShipCouchbaseRepository(ShipCouchbaseRepository shipCouchbaseRepository) {
        this.shipCouchbaseRepository = shipCouchbaseRepository;
    }

    public CompanyCouchbaseRepository(List<Constraint<CompanyEntity>> constraints, Class<CompanyEntity> companyEntityClass, String entityType) {
        super(constraints, companyEntityClass, entityType);
    }

    @Override
    public void delete(String id, String userId) {
        List<ShipDto> relatedShips = findRelatedShips(Arrays.asList(id));
        if (relatedShips.isEmpty()){
            super.delete(id, userId);
        } else {
            CompanyEntity company = findOne(id);
            Map<String, List<String>> relatedObjects = new HashMap<>();
            List<String> shipIdsList = relatedShips.stream()
                    .map(ShipDto::getId)
                    .collect(Collectors.toList());
            List<String> shipNamesList = relatedShips.stream()
                    .map(shipDto -> shipDto.getShip().getShipIdentity().getName())
                    .collect(Collectors.toList());
            relatedObjects.put(ShipEntity.SHIP, shipIdsList);

            String message = "Can't delete Company with name " + company.getCompany().getName() +
                    "because it has related Ships: " +
                    shipNamesList.stream().collect(Collectors.joining(", ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }

    private List<ShipDto> findRelatedShips(List<String> companyIds){
        List<ShipDto> relatedShips = shipCouchbaseRepository.findShipsRelatedToCompanies(companyIds);
        return relatedShips;
    }
}
