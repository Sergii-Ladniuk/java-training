package com.trident.apis.entitymanager.controller;


import com.trident.apis.entitymanager.model.CodeDictionary;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/apis/codedictionary", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/apis/codedictionary", description = "CRUD operations with Code Dictionaries")
public class CodeDictionaryEntityController {
    
    @Autowired
    private RepositoryDecorator<CodeDictionary> repositoryDecorator;

    @ApiOperation(value = "Loads Code Dictionary list", response = CodeDictionary[].class, httpMethod = "GET")
    @GetMapping
    public Iterable<CodeDictionary> all() {
        return repositoryDecorator.findAll();
    }

    @ApiOperation(value = "Load Code Dictionary by ID", response = CodeDictionary.class, httpMethod = "GET")
    @GetMapping(value = "{id}")
    public CodeDictionary getById(@PathVariable(name = "id") String id) {
        return repositoryDecorator.findOne(id);
    }

    @ApiOperation(value = "Save new Code Dictionary", response = CodeDictionary.class, httpMethod = "POST")
    @PostMapping(value = {"", "{id}"})
    public CodeDictionary insert(@RequestBody @Valid CodeDictionary codeDictionary, HttpServletRequest request) {
        checkIfIdSet(codeDictionary);
        return repositoryDecorator.insert(codeDictionary, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Update Code Dictionary", response = CodeDictionary.class, httpMethod = "PUT")
    @PutMapping(value = {"", "{id}"})
    public CodeDictionary update(@RequestBody @Valid CodeDictionary codeDictionary, HttpServletRequest request) {
        checkIfIdSet(codeDictionary);
        return repositoryDecorator.update(codeDictionary, RestUtils.getUserId(request));
    }

    @ApiOperation(value = "Delete Code Dictionary by ID", response = SimpleResponse.class, httpMethod = "DELETE")
    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public SimpleResponse delete(@PathVariable("id") String id, HttpServletRequest request) {
        repositoryDecorator.delete(id, RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    @ApiOperation(value = "Returns history by Code Dictionary id", httpMethod = "GET")
    @GetMapping(value = "history/{id}")
    public List<HistoryEntity<?>> getHistory(@PathVariable("id") String id) {
        return repositoryDecorator.getHistoryById(id);
    }

    private void checkIfIdSet(CodeDictionary codeDictionary) {
        if (codeDictionary.getId() == null) {
            throw new IllegalArgumentException("Provide Code Dictionary with ID");
        }
    }

}
