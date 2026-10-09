package com.flowforge.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler({
                        BackendUnavailableException.class,
                        IllegalStateException.class
        })
        public ResponseEntity<Map<String, String>> handleBackendFailure(
                        RuntimeException exception) {

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(Map.of(
                                                "error", "Service Unavailable",
                                                "message", exception.getMessage()));
        }
}