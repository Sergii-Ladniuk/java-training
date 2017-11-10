package com.trident.apis.entitymanager.controller;

import com.trident.apis.entitymanager.repository.TestModeRepository;
import com.trident.shared.immigration.network.SimpleResponse;
import io.swagger.annotations.ApiOperation;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-mode")
public class TestModeController {

    @Autowired @Setter
    private TestModeRepository testModeRepository;

    @ApiOperation("Set test mode")
    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public SimpleResponse setTestMode(@RequestParam("enabled") Boolean enabled) {
        testModeRepository.save(enabled);
        return new SimpleResponse("OK");
    }
}
