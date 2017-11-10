package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.model.ApisTemplate;
import com.trident.apis.entitymanager.model.DocumentContent;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.apis.entitymanager.model.Port;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.criteria.*;
import com.trident.shared.immigration.util.ImmigrationStringUtils;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1", produces = APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/ports", description = "CRUD opetations with ports")
public class PortEntityController {

    private static Logger logger = Logger.getLogger(PortEntityController.class);

    @Autowired
    private RepositoryDecorator<Port> repository;
    @Lazy
    @Autowired
    private RepositoryDecorator<DocumentContent> contentRepository;

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load ports list", httpMethod = "GET")
    @RequestMapping("ports")
    public Iterable<Port> allPorts(
            Sort sort,
            @ApiParam(value = FILTER_BY_NAME, name = NAME)
            @RequestParam(required = false) String name,
            @ApiParam(value = FILTER_BY_CODE, name = CODE)
            @RequestParam(required = false) String code,
            @ApiParam(value = FILTER_BY_COUNTRY, name = COUNTRY_ID)
            @RequestParam(required = false, name = COUNTRY_ID) String countryId) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, code, countryId);
        return repository.findAll(sort, searchQuery);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = PAGE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = PAGE_RESULTS),
            @ApiImplicitParam(name = SIZE, dataType = INTEGER, paramType = QUERY_PARAM_TYPE, value = NUMBER_OF_RECORDS_PER_PAGE),
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Load ports page", httpMethod = "GET")
    @RequestMapping("ports/page")
    public Page<Port> allPorts(
            Pageable pageable,
            @ApiParam(FILTER_BY_NAME)
            @RequestParam(required = false) String name,
            @ApiParam(FILTER_BY_CODE)
            @RequestParam(required = false) String code,
            @ApiParam(value = FILTER_BY_COUNTRY, name = COUNTRY_ID)
            @RequestParam(required = false, name = COUNTRY_ID) String countryId) {
        SearchCriteriaList searchQuery = getSearchCriterias(name, code, countryId);
        return repository.findAll(pageable, searchQuery);
    }

    private SearchCriteriaList getSearchCriterias(String name, String code, String countryId) {
        return new SearchCriteriaList(
                new SearchLikeCriteria(NAME, ImmigrationStringUtils.decodeParam(name)),
                new SearchLikeCriteria(CODE, code),
                new SearchEqualCriteria<>(COUNTRY_ID, countryId));
    }

    @ApiOperation(value = "Load a port by ID", response = Port.class, httpMethod = "GET")
    @GetMapping("ports/{portId}")
    public Port portById(@PathVariable("portId") String portId) {
        return repository.findOne(portId);
    }

    @ApiOperation(value = "Load a port list by port IDs", response = Port[].class, httpMethod = "GET")
    @RequestMapping(value = "ports/listByIds", method = RequestMethod.GET)
    public Iterable<Port> portsById(@RequestParam("id") List<String> portIds) {
        return repository.findAll(new SearchCriteriaList(new SearchIdInCriteria(portIds)));
    }

    @ApiOperation(value = "Save a new port", response = Port.class, httpMethod = "POST")
    @RequestMapping(value = "ports", method = RequestMethod.POST)
    public Port insertPort(@RequestBody @Valid Port port, HttpServletRequest request) {
        makeSureIdPresent(port);
        processNotes(port, RestUtils.getUserId(request));
        return repository.insert(port, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Save multiple ports", response = Port[].class, httpMethod = "POST")
    @RequestMapping(value = "ports/list", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public Iterable<Port> insertPorts(@RequestBody List<Port> ports, HttpServletRequest request) {
        logger.debug("Save multiple ports started.");
        try {
            ports.forEach(port -> {
                makeSureIdPresent(port);
                processNotes(port, RestUtils.getUserId(request));
            });
            return repository.bulkSaveWithoutHistory(ports, RestUtils.getUserId(request));
        } finally {
            logger.debug("Save multiple ports finished.");
        }
    }

    @ApiOperation(value = "Update a port", response = Port.class, httpMethod = "PUT")
    @RequestMapping(value = {"ports", "ports/{portId}"}, method = RequestMethod.PUT)
    public Port updatePort(@RequestBody @Valid Port port, HttpServletRequest request) {
        makeSureIdPresent(port);
        processNotes(port, RestUtils.getUserId(request));
        return repository.update(port, RestUtils.getUserId(request));
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = SORT, allowMultiple = true, dataType = STRING, paramType = QUERY_PARAM_TYPE,
                    value = SORT_PARAM_DESCRIPTION)
    })
    @ApiOperation(value = "Loads ports from a country with ID specified", response = Port.class, httpMethod = "GET")
    @RequestMapping({"countries/{countryId}/ports", "ports/country/{countryId}"})
    public List<Port> portsByCountry(@PathVariable(name = "countryId") String countryId, Sort sort) {
        return repository.findAll(
                sort,
                new SearchCriteriaList(new SearchEqualCriteria<>(COUNTRY_ID, countryId))
        );
    }

    @ApiOperation(value = "Delete a port", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "ports/{portId}", method = RequestMethod.DELETE)
    public SimpleResponse deletePort(@PathVariable("portId") String portId, HttpServletRequest request) {
        repository.delete(portId, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Delete all ports", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "ports", method = RequestMethod.DELETE)
    public SimpleResponse deleteAll(HttpServletRequest request) {
        repository.deleteAll(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by Port id", httpMethod = "GET")
    @GetMapping(value = "ports/history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repository.getHistoryById(id);
    }

    private Port savePost(Port port, String userId) {
        makeSureIdPresent(port);
        return repository.save(port, userId);
    }

    private void makeSureIdPresent(Port port) {
        if (port.getId() == null) {
            port.setId(UUID.randomUUID().toString());
        }
    }

    private void processNotes(Port port, String userId) {
        if (port.getNotes() != null) {
            DocumentContent notes;
            if (port.getRichFormatNotes() != null && port.getRichFormatNotes()) {
                notes = contentRepository.insert(
                        DocumentContent.fromRichFormat(
                                port.getNotes(),
                                port.getNoteFileName()),
                        userId);
            } else {
                notes = contentRepository.insert(DocumentContent.fromPlainText(
                        port.getNotes()),
                        userId);
            }
            port.setNoteId(notes.getId());
            port.setNotes(null);
        }
    }
}
