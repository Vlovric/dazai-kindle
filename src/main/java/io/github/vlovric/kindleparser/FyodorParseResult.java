package io.github.vlovric.kindleparser;

import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.kindleparser.models.Clipping;

public record FyodorParseResult(
        List<Clipping> clippings,
        Path outputDir,
        Path selectedFile,
        String selectedBookTitle
) {
}
