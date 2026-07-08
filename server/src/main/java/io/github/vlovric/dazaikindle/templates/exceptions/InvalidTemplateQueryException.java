package io.github.vlovric.dazaikindle.templates.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class InvalidTemplateQueryException extends ApiException {
    public InvalidTemplateQueryException(String param, String value) {
        super(HttpStatus.BAD_REQUEST, "Invalid " + param + " value: " + value);
    }
}
