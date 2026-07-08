package io.github.vlovric.dazaikindle.runs;

import io.github.vlovric.dazaikindle.runs.exceptions.InvalidRunQueryException;

/**
 * The four fields GET /runs can sort by. NAME/AUTHOR compare case-insensitively;
 * HIGHLIGHTS/LAST_MODIFIED compare numerically/temporally.
 */
public enum RunSort {
    NAME,
    AUTHOR,
    HIGHLIGHTS,
    LAST_MODIFIED;

    public static RunSort fromQueryParam(String value) {
        if (value == null || value.isBlank()) {
            return LAST_MODIFIED;
        }
        return switch (value) {
            case "name" -> NAME;
            case "author" -> AUTHOR;
            case "highlights" -> HIGHLIGHTS;
            case "lastModified" -> LAST_MODIFIED;
            default -> throw new InvalidRunQueryException("sort", value);
        };
    }
}
