package io.github.vlovric.dazaikindle.runs.dto;

public record RunExportDownload(
    byte[] content,
    String filename
) {}
