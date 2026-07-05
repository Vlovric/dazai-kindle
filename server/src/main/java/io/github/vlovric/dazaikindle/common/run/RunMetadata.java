package io.github.vlovric.dazaikindle.common.run;

/**
 * Persisted as run.json in each run's library folder. Only holds data that
 * can't be derived from the filesystem (author, highlightCount) - artifact
 * lists and lastModified are computed on read from the folder contents.
 */
public record RunMetadata(String author, long highlightCount) {
}
