package io.github.vlovric.dazaikindle.runs.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class InvalidRunQueryException extends ApiException {
    public InvalidRunQueryException(String param, String value) {
        super(HttpStatus.BAD_REQUEST, "Invalid " + param + " value: " + value);
    }
}
