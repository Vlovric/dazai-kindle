package io.github.vlovric.dazaikindle.files.dto;

import java.util.List;

public record FileListResponse(
    List<UploadedFileResponse> files,
    int totalPages,
    int currentPage
) {}
