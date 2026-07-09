package io.github.vlovric.dazaikindle.runs.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;

import io.github.vlovric.dazaikindle.common.exception.ApiException;

public class ArtifactNotFoundException extends ApiException {
    public ArtifactNotFoundException(String runName, List<String> artifactNames) {
        super(HttpStatus.NOT_FOUND,
            "Unknown artifact(s) for run '" + runName + "': " + String.join(", ", artifactNames));
    }
}
