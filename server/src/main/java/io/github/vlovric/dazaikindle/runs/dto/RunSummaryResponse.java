package io.github.vlovric.dazaikindle.runs.dto;

import java.time.Instant;

public record RunSummaryResponse(
    String name,
    String author,
    long highlightCount,
    Instant lastModified
) {}
