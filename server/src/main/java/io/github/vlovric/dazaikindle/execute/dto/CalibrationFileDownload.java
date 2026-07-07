package io.github.vlovric.dazaikindle.execute.dto;

public record CalibrationFileDownload(
    byte[] content,
    String filename
) {}
