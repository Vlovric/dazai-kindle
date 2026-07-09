package io.github.vlovric.dazaikindle.templates.dto;

import java.time.Instant;

public record TemplateResponse(String name, String type, Instant lastModified) {}
