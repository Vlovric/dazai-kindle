package io.github.vlovric.dazaikindle.runs.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class ArtifactOpenException extends ApiException {
    public ArtifactOpenException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
