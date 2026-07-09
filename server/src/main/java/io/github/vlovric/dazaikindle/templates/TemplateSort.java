package io.github.vlovric.dazaikindle.templates;

import io.github.vlovric.dazaikindle.templates.exceptions.InvalidTemplateQueryException;

public enum TemplateSort {
    NAME,
    LAST_MODIFIED;

    public static TemplateSort fromQueryParam(String value) {
        if (value == null || value.isBlank()) {
            return LAST_MODIFIED;
        }
        return switch (value) {
            case "name" -> NAME;
            case "lastModified" -> LAST_MODIFIED;
            default -> throw new InvalidTemplateQueryException("sort", value);
        };
    }
}
