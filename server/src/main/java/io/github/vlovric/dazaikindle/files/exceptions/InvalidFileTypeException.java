package io.github.vlovric.dazaikindle.files.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class InvalidFileTypeException extends ApiException {

    private InvalidFileTypeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public static InvalidFileTypeException forQueryParam(String type) {
        return new InvalidFileTypeException("Invalid or missing file type: " + type);
    }

    public static InvalidFileTypeException forExtension(String filename) {
        return new InvalidFileTypeException("Unsupported file extension: " + filename);
    }

    public static InvalidFileTypeException forTemplateNaming(String filename, boolean expectedHeading) {
        String expected = expectedHeading ? "must end in \"_h\"" : "must not end in \"_h\"";
        return new InvalidFileTypeException("Template filename " + expected + ": " + filename);
    }
}
