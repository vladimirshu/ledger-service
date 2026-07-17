package com.ledgerservice.controller;

import com.ledgerservice.service.TransferException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

record ApiError(String code, String message) {
}

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TransferException.class)
    ResponseEntity<ApiError> handleTransferException(TransferException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "ACCOUNT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INSUFFICIENT_FUNDS", "IDEMPOTENCY_CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ApiError(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleInvalidParameters(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "Request parameters are invalid"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConcurrentUpdate(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONCURRENT_UPDATE", "An account changed concurrently; retry the request"));
    }
}
