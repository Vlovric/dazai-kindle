package io.github.vlovric.dazaikindle.clippings.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class ClippingsNotFoundException extends ApiException {
    public ClippingsNotFoundException() {
        super(HttpStatus.NOT_FOUND, "No clippings file has been uploaded yet");
    }
}
