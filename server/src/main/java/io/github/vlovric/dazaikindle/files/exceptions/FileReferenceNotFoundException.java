package io.github.vlovric.dazaikindle.files.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class FileReferenceNotFoundException extends ApiException {
    public FileReferenceNotFoundException(String ref) {
        super(HttpStatus.NOT_FOUND, "File reference not found: " + ref);
    }
}
