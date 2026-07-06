package io.github.vlovric.dazaikindle.common.run.exceptions;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class DraftNotFoundException extends ApiException {
    public DraftNotFoundException(String draftId) {
        super(HttpStatus.NOT_FOUND, "Unknown draft: " + draftId);
    }
}
