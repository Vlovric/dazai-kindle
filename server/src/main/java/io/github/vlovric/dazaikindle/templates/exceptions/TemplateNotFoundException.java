package io.github.vlovric.dazaikindle.templates.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class TemplateNotFoundException extends ApiException {
    public TemplateNotFoundException(List<String> names) {
        super(HttpStatus.NOT_FOUND, "Unknown template(s): " + String.join(", ", names));
    }
}
