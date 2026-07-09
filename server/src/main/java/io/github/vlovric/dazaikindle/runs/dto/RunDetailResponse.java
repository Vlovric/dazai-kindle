package io.github.vlovric.dazaikindle.runs.dto;

import java.time.Instant;
import java.util.Map;

/**
 * artifacts is keyed by "book" | "calibration" | "output" | "headingsOutput" |
 * "debugRun" - a key is absent entirely if that run doesn't have that artifact
 * (e.g. a full run with no prior headings run merged into it has no
 * "headingsOutput" entry).
 */
public record RunDetailResponse(
    String name,
    String author,
    long highlightCount,
    Instant lastModified,
    Map<String, ArtifactResponse> artifacts
) {}
