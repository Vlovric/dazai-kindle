package io.github.vlovric.kindleparser.models;

/**
 * A single entry from the book's Table of Contents.
 */
public record TocEntry(
    String title,
    String file,
    String anchor,
    int level
) {
}
