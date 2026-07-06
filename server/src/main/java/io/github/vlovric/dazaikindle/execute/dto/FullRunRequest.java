package io.github.vlovric.dazaikindle.execute.dto;

public record FullRunRequest(
    String bookRef,
    String calibrationRef,
    String clippingsRef,
    String templateRef,
    String title,
    boolean debugMode,
    boolean overwriteFyodorTemplate
) {}
