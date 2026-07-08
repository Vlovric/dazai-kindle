package io.github.vlovric.dazaikindle.templates.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class TemplateRenderException extends ApiException {
    public TemplateRenderException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Template could not be rendered: " + message);
    }
}
