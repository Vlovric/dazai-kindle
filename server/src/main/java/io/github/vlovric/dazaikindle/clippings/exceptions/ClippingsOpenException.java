package io.github.vlovric.dazaikindle.clippings.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class ClippingsOpenException extends ApiException {
    public ClippingsOpenException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
