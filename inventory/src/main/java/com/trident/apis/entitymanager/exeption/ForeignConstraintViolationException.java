package com.trident.apis.entitymanager.exeption;

import com.trident.shared.immigration.network.ForeignConstraintErrorResponse;

import java.util.List;
import java.util.Map;

/**
 * Created by vdmitrovskiy on 7/24/17.
 */
public class ForeignConstraintViolationException extends RuntimeException{

    public ForeignConstraintViolationException(String message) {
        super(message);
    }

    private Map<String, List<String>> relatedObjectsMap;

    public Map<String, List<String>> getRelatedObjectsMap() {
        return relatedObjectsMap;
    }

    public void setRelatedObjectsMap(Map<String, List<String>> relatedObjectsMap) {
        this.relatedObjectsMap = relatedObjectsMap;
    }
}
