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
    public String title() { return tocEntry.title(); }
    public String file() { return tocEntry.file(); }
    public String anchor() { return tocEntry.anchor(); }
    public int level() { return tocEntry.level(); }
}
