package io.github.vlovric.dazaikindle.execute.dto;

public record CalibrationRunRequest(
    String bookRef,
    boolean debugMode
) {}
