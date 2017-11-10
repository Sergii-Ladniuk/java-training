package com.trident.apis.entitymanager.controller;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.trident.shared.immigration.constants.ControllerConstants;
import com.trident.shared.immigration.network.SimpleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/v1/query", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class QueryEntityController {

    @Qualifier("bucketWithRetry")
    @Autowired
    private Bucket bucket;
    @Autowired
    private Environment environment;

    @PostMapping
    public ResponseEntity<String> execute(@RequestBody String query) {
        if (!isProd()) {
            N1qlQueryResult query1 = bucket.query(N1qlQuery.simple(query));
            return new ResponseEntity<>(
                    query1.status() +
                            query1.errors().stream()
                                    .map(JsonObject::toString)
                                    .collect(Collectors.joining(",\n", "\nErrors: ", "\n")) +
                            query1.allRows().stream()
                                    .map(row -> row.value().toString())
                                    .collect(Collectors.joining("Result:", ",\n", "\n\n")),
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Operation not allowed on prod environment", HttpStatus.FORBIDDEN);
        }
    }

    public boolean isProd() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(ControllerConstants.PROD_ENVIRONMENT_NAME::equalsIgnoreCase);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll() {
        if (!isProd()) {
            N1qlQueryResult query = bucket.query(N1qlQuery.simple("delete from " + bucket.name() + " where __type like 'dive_%'"));
            return new ResponseEntity<>(new SimpleResponse("Result : " + query.status() + " Rows: " + query.rows() + " Errors:" + query.errors()), HttpStatus.OK);
        }else {
            return new ResponseEntity<>("Operation not allowed on prod environment", HttpStatus.FORBIDDEN);
        }
    }

}
