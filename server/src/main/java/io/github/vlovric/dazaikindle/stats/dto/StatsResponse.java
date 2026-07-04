package io.github.vlovric.dazaikindle.stats.dto;

import java.time.Instant;

public record StatsResponse(
    int highlightCount,
    int entryCount,
    Instant lastRunTime
) {}
