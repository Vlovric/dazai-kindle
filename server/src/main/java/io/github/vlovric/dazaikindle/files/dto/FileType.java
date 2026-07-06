package io.github.vlovric.dazaikindle.files.dto;

import java.util.Set;

import io.github.vlovric.dazaikindle.files.exceptions.InvalidFileTypeException;

/**
 * The four artifact kinds resolvable from a run/draft folder. Clippings are
 * deliberately not a FileType - they're a single fixed file, not per-run.
 */
public enum FileType {

    BOOK("book", Set.of(".epub", ".azw3", ".mobi")),
    CALIBRATION("calibration", Set.of(".txt")),
    TEMPLATE("template", Set.of(".ftl")),
    HEADING_TEMPLATE("headings-template", Set.of(".ftl"));

    private final String baseName;
    private final Set<String> allowedExtensions;

    FileType(String baseName, Set<String> allowedExtensions) {
        this.baseName = baseName;
        this.allowedExtensions = allowedExtensions;
    }

    public String baseName() {
        return baseName;
    }

    public boolean isAllowedExtension(String extension) {
        return allowedExtensions.contains(extension.toLowerCase());
    }

    /**
     * Maps the GET /files?type= query param value to a FileType.
     */
    public static FileType fromQueryParam(String type) {
        return switch (type) {
            case "book" -> BOOK;
            case "calibration" -> CALIBRATION;
            case "template" -> TEMPLATE;
            case "headingTemplate" -> HEADING_TEMPLATE;
            case null, default -> throw InvalidFileTypeException.forQueryParam(type);
        };
    }
}
