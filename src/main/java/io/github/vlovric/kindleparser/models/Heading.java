package io.github.vlovric.kindleparser.models;

/**
 * A TocEntry that has been resolved to a Kindle location.
 * Uses composition to wrap the original TocEntry.
 */
public record Heading(
    TocEntry tocEntry,
    int charOffset,
    int location
) {
    // Convenience delegations

    /**
     * @return the title of the heading from the TOC
     */
    public String title() { return tocEntry.title(); }

    /**
     * @return the file path containing the heading within the EPUB
     */
    public String file() { return tocEntry.file(); }

    /**
     * @return the HTML anchor ID of the heading, if any
     */
    public String anchor() { return tocEntry.anchor(); }

    /**
     * @return the hierarchical level of the heading (e.g., 1 for H1)
     */
    public int level() { return tocEntry.level(); }
}
