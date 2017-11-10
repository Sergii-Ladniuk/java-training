package com.trident.apis.entitymanager.repository;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.apis.entitymanager.model.Country;
import com.trident.apis.entitymanager.model.Port;
import com.trident.apis.entitymanager.model.ShipEntity;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.shared.immigration.api.client.tdrs.TdrsRuleRepository;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipDto;
import com.trident.shared.immigration.dto.apis.knowledge.ship.ShipLegal;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.criteria.SearchCriteriaList;
import com.trident.shared.immigration.repository.criteria.SearchEqualCriteria;
import com.trident.shared.immigration.repository.criteria.SearchInArrayFieldCriteria;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vdmitrovskiy on 7/27/17.
 */
public class CountryCouchbaseRepository extends RepositoryDecorator<Country> {

    @Autowired
    private RepositoryDecorator<TdrsRule> tdrsRuleRepositoryDecorator;
    private ShipCouchbaseRepository shipCouchbaseRepository;
    private PortCouchbaseRepository portCouchbaseRepository;

    @Autowired
    public void setShipCouchbaseRepository(ShipCouchbaseRepository shipCouchbaseRepository) {
        this.shipCouchbaseRepository = shipCouchbaseRepository;
    }
    @Autowired
    public void setPortCouchbaseRepository(PortCouchbaseRepository portCouchbaseRepository) {
        this.portCouchbaseRepository = portCouchbaseRepository;
    }

    public CountryCouchbaseRepository(List<Constraint<Country>> constraints, Class<Country> countryClass, String entityType) {
        super(constraints, countryClass, entityType);
    }

    @Override
    public void delete(String id, String userId) {

        Map<String, List<String>> relatedObjects = new HashMap<>();

        List<TdrsRule> relatedTdrsRulesByDestination = tdrsRuleRepositoryDecorator.findAll(
                new SearchCriteriaList(new SearchEqualCriteria<>("destinationCountryId", id)));

        List<TdrsRule> relatedTdrsRulesByCitizenship = tdrsRuleRepositoryDecorator.findAll(
                new SearchCriteriaList((new SearchInArrayFieldCriteria<>("citizenshipCountryIds", id))));

        List<ShipEntity> relatedShipBuildInfo = shipCouchbaseRepository.findAll(
                new SearchCriteriaList(new SearchEqualCriteria<>("ship.shipTechnicalSpec.shipBuildInfo.builtAtCountryId", id)));

        List<ShipEntity> relatedShipLegal = shipCouchbaseRepository.findAll(
                new SearchCriteriaList(new SearchEqualCriteria<>("ship.shipLegal.nationalityCountryId", id)));

        List<Port> relatedPorts = portCouchbaseRepository.findAll(
                new SearchCriteriaList(new SearchEqualCriteria<>("countryId", id)));

        if (relatedTdrsRulesByCitizenship.isEmpty() &&
                relatedTdrsRulesByDestination.isEmpty() &&
                relatedShipBuildInfo.isEmpty() &&
                relatedShipLegal.isEmpty() &&
                relatedPorts.isEmpty()){
            super.delete(id, userId);
        } else {
            Country country = findOne(id);

            List<String> messageParts = new ArrayList<>();
            int i = 1;

            if (!relatedTdrsRulesByCitizenship.isEmpty()){
                List<String> tdrsRuleIdsByCitizenship = relatedTdrsRulesByCitizenship.stream()
                        .map(TdrsRule::getId)
                        .collect(Collectors.toList());
                List<String> tdrsRuleNamesByCitizenship = relatedTdrsRulesByCitizenship.stream()
                        .map(TdrsRule::getName)
                        .collect(Collectors.toList());
                relatedObjects.put(TdrsRule.TDRS_RULE, tdrsRuleIdsByCitizenship);

                String message = i + ") it has related TDRS Rules by Citizenship Country: "
                        + tdrsRuleNamesByCitizenship.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedTdrsRulesByDestination.isEmpty()){
                List<String> tdrsRuleIdsByDestination = relatedTdrsRulesByDestination.stream()
                        .map(TdrsRule::getId)
                        .collect(Collectors.toList());
                List<String> tdrsRuleNameByDestination = relatedTdrsRulesByDestination.stream()
                        .map(TdrsRule::getName)
                        .collect(Collectors.toList());
                if(relatedObjects.containsKey(TdrsRule.TDRS_RULE)){
                    relatedObjects.put(TdrsRule.TDRS_RULE, Stream.concat(relatedObjects.get(TdrsRule.TDRS_RULE).stream(),
                                                                            tdrsRuleIdsByDestination.stream())
                                                                    .collect(Collectors.toList()));
                } else {
                    relatedObjects.put(TdrsRule.TDRS_RULE, tdrsRuleIdsByDestination);
                }

                String message = i + ") it has related TDRS Rules by Destination Country: "
                        + tdrsRuleNameByDestination.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedShipBuildInfo.isEmpty()){
                List<String> shipEntityByShipBuildInfoIds = relatedShipBuildInfo.stream()
                        .map(ShipEntity::getId)
                        .collect(Collectors.toList());
                List<String> shipEntityByShipBuildInfoNames = relatedShipBuildInfo.stream()
                        .map(shipEntity -> shipEntity.getShip().getShipIdentity().getName())
                        .collect(Collectors.toList());
                relatedObjects.put(ShipEntity.SHIP, shipEntityByShipBuildInfoIds);

                String message = i + ") it has related Ships by Ship Build Info's: "
                        + shipEntityByShipBuildInfoNames.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedShipLegal.isEmpty()){
                List<String> shipEntityByShipLegalIds = relatedShipLegal.stream()
                        .map(ShipEntity::getId)
                        .collect(Collectors.toList());
                List<String> shipEntityByShipLegalNames = relatedShipLegal.stream()
                        .map(shipEntity -> shipEntity.getShip().getShipIdentity().getName())
                        .collect(Collectors.toList());

                if(relatedObjects.containsKey(ShipEntity.SHIP)){
                    relatedObjects.put(ShipEntity.SHIP, Stream.concat(relatedObjects.get(ShipEntity.SHIP).stream(),
                            shipEntityByShipLegalIds.stream())
                            .collect(Collectors.toList()));
                } else {
                    relatedObjects.put(ShipEntity.SHIP, shipEntityByShipLegalIds);
                }

                String message = i + ") it has related Ships by Ship Legals: "
                        + shipEntityByShipLegalNames.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            if (!relatedPorts.isEmpty()){
                List<String> portIds = relatedPorts.stream()
                        .map(Port::getId)
                        .collect(Collectors.toList());
                relatedObjects.put(Port.PORT, portIds);
                List<String> portNames = relatedPorts.stream()
                        .map(Port::getName)
                        .collect(Collectors.toList());
                relatedObjects.put(Port.PORT, portIds);

                String message = i + ") it has related Ports: "
                        + portNames.stream().collect(Collectors.joining(", "));
                messageParts.add(message);
                i++;
            }

            String message = "Can't delete Country " + country.getName() +
                    " (code: " + country.getCode() + ") because: " +
                    messageParts.stream().collect(Collectors.joining("; ")) +
                    ". Please remove them first.";

            ForeignConstraintViolationException exception = new ForeignConstraintViolationException(message);
            exception.setRelatedObjectsMap(relatedObjects);
            throw exception;
        }
    }
}
