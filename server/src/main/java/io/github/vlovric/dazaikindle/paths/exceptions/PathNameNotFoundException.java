package io.github.vlovric.dazaikindle.paths.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class PathNameNotFoundException extends ApiException {
    public PathNameNotFoundException(String name) {
        super(HttpStatus.NOT_FOUND, "Unknown path name: " + name);
    }
}
