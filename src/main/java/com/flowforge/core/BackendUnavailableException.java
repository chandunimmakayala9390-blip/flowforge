package com.flowforge.core;

public class BackendUnavailableException extends RuntimeException {

    public BackendUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
