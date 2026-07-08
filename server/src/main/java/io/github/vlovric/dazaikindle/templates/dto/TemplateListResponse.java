package io.github.vlovric.dazaikindle.templates.dto;

import java.util.List;

public record TemplateListResponse(
    List<TemplateResponse> templates,
    int totalPages,
    int currentPage
) {}
