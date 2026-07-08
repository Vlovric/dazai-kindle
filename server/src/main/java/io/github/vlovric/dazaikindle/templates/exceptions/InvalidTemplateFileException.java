package io.github.vlovric.dazaikindle.templates.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class InvalidTemplateFileException extends ApiException {
    public InvalidTemplateFileException(String filename) {
        super(HttpStatus.BAD_REQUEST, "Unsupported file: " + filename);
    }
}
