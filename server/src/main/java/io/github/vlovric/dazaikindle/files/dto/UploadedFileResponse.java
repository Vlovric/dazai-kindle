package io.github.vlovric.dazaikindle.files.dto;

import java.time.Instant;

/**
 * draftId is populated on upload responses for book/calibration/template/
 * headingsTemplate (the client must pass it back on later uploads and in
 * FullRunRequest's refs). It's null for clippings uploads and for entries
 * returned by GET /files (those name a completed run, not a draft).
 */
public record UploadedFileResponse(
    String name,
    Instant lastModified,
    String draftId
) {}
