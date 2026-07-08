package io.github.vlovric.dazaikindle.execute.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class PipelineExecutionException extends ApiException {
    public PipelineExecutionException(Exception cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Pipeline execution failed: " + cause.getMessage());
    }
}
