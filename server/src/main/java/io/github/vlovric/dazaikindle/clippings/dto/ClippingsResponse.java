package io.github.vlovric.dazaikindle.clippings.dto;

import java.time.Instant;

public record ClippingsResponse(
    String name,
    Instant uploadedAt,
    String path
) {}
