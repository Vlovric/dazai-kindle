package io.github.vlovric.dazaikindle.fyodor;

import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.dazaikindle.models.Clipping;

public record FyodorParseResult(
        List<Clipping> clippings,
        Path outputDir,
        Path selectedFile,
        String selectedBookTitle,
        String fyodorStdout,
        List<Path> outputFiles
) {
}
