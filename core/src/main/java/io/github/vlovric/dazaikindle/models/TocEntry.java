package io.github.vlovric.dazaikindle.models;

/**
 * A single entry from the book's Table of Contents.
 * Parsed from the EPUB's toc.ncx (EPUB2) or nav.xhtml (EPUB3) file
 */
public record TocEntry(
    String title,
    String file,
    String anchor,
    int level
) {
}
