package io.github.vlovric.dazaikindle.runs.dto;

import java.util.List;

public record RunListResponse(
    List<RunSummaryResponse> runs,
    int totalPages,
    int currentPage
) {}
