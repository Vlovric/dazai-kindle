package io.github.vlovric.dazaikindle.execute.dto;

public record HeadingsRunRequest(
    String bookRef,
    String calibrationRef,
    String headingsTemplateRef,
    boolean debugMode
) {}
