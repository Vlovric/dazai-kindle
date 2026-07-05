package io.github.vlovric.dazaikindle.paths.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class InvalidPathException extends ApiException {
    public InvalidPathException(String path) {
        super(HttpStatus.BAD_REQUEST, "Path does not exist or is not accessible: " + path);
    }
}
