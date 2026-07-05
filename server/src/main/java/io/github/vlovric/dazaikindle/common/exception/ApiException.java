package io.github.vlovric.dazaikindle.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for exceptions meant to surface a specific status and message to the
 * client. Caught centrally by GlobalExceptionHandler - controllers don't need
 * their own @ExceptionHandler methods for these.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
