package io.github.vlovric.dazaikindle.pipeline;

/**
 * 
 * PipelineResult is a record that encapsulates the result of running the pipeline.
 * It contains the title and author of the processed book, as well as the count of highlights extracted from it.
 * The server uses it to store important metadata about each run on the filesystem
 * @param bookTitle
 * @param author
 * @param highlightCount
 */
public record PipelineResult(
    String bookTitle,
    String author,
    long highlightCount
){}
