package com.trident.apis.entitymanager.errorhandling;

import com.trident.apis.entitymanager.exeption.ForeignConstraintViolationException;
import com.trident.shared.immigration.correlationid.CorrelationIdService;
import com.trident.shared.immigration.network.ForeignConstraintErrorResponse;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by vdmitrovskiy on 7/24/17.
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ForeignConstraintViolationExceptionHandler {

    private static final Logger logger = Logger.getLogger(ForeignConstraintViolationException.class);

    @Autowired
    private CorrelationIdService correlationIdService;

    @ExceptionHandler(ForeignConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity handleException(HttpServletRequest request, Exception exception) {
        logger.error("ForeignConstraintViolationException. Correlation ID: " + correlationIdService.getCorrelationId(), exception);

        ForeignConstraintViolationException foreignConstraintViolationException =
                (ForeignConstraintViolationException) exception;

        ForeignConstraintErrorResponse body = (ForeignConstraintErrorResponse) new ForeignConstraintErrorResponse()
                .setRelatedObjectsMap(foreignConstraintViolationException.getRelatedObjectsMap())
                .setHttpStatus(409)
                .setError(foreignConstraintViolationException.getMessage())
                .setErrorCode("409")
                .setCorrelationId(correlationIdService.getCorrelationId())
                .setErrorDetail("Failed due to remote error: " + foreignConstraintViolationException.getMessage());

        return new ResponseEntity<>(body, HttpStatus.valueOf(409));
    }
}
