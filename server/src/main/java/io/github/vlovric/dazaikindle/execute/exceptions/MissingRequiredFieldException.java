package io.github.vlovric.dazaikindle.execute.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class MissingRequiredFieldException extends ApiException {
    public MissingRequiredFieldException(String field) {
        super(HttpStatus.BAD_REQUEST, "Missing required field: " + field);
    }
}
