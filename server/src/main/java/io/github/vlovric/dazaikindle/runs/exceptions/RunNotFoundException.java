package io.github.vlovric.dazaikindle.runs.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class RunNotFoundException extends ApiException {
    public RunNotFoundException(List<String> names) {
        super(HttpStatus.NOT_FOUND, "Unknown run(s): " + String.join(", ", names));
    }
}
