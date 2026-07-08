package io.github.vlovric.dazaikindle.templates;

import java.util.Optional;

import io.github.vlovric.dazaikindle.templates.exceptions.InvalidTemplateQueryException;

/**
 * Per FR09: a template's filename decides its kind - one ending in "_h"
 * (before the extension) is a heading template, everything else is an
 * output template.
 */
public enum TemplateType {
    OUTPUT,
    HEADING;

    public static Optional<TemplateType> fromQueryParam(String value) {
        if (value == null || value.isBlank() || value.equals("all")) {
            return Optional.empty();
        }
        return switch (value) {
            case "output" -> Optional.of(OUTPUT);
            case "heading" -> Optional.of(HEADING);
            default -> throw new InvalidTemplateQueryException("type", value);
        };
    }

    public String toQueryValue() {
        return this == HEADING ? "heading" : "output";
    }
}
