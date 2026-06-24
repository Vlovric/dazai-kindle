package io.github.vlovric.dazaikindle.toc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.github.vlovric.dazaikindle.EpubLoader;
import io.github.vlovric.dazaikindle.models.TocEntry;

public class TocParserResolver {

    private final EpubLoader loader;

    public TocParserResolver(EpubLoader loader) {
        this.loader = loader;
    }

    /**
     * Reads the TOC file denoted by the loader and extracts its hierarchical entries.
     * Delegates parsing logic gracefully based on the TOC file extension.
     *
     * @return an ordered list of Table of Contents entries matching the book's navigation structure
     * @throws IOException if reading or parsing the TOC fails
     */
    public List<TocEntry> parse() throws IOException {
        String tocHref = loader.getTocHref();
        if (tocHref == null || tocHref.isEmpty()) {
            return new ArrayList<>();
        }

        String tocPath = loader.resolve(tocHref);
        byte[] raw = loader.read(tocPath);
        String content = new String(raw, StandardCharsets.UTF_8);

        String filename = Paths.get(tocPath).getFileName().toString();
        TocParser parser = TocParserFactory.forFile(filename);
        
        List<TocEntry> entries = parser.parse(content);

        return resolvePaths(entries);
    }

    private List<TocEntry> resolvePaths(List<TocEntry> entries) {
    return entries.stream()
        .map(e -> new TocEntry(
            e.title(),
            e.file().isEmpty() ? "" : loader.resolve(e.file()),
            e.anchor(),
            e.level()
        ))
        .toList();
    }
    
}
