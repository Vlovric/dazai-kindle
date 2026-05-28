package io.github.vlovric.kindleparser.toc;

import java.io.IOException;
import java.util.List;

import io.github.vlovric.kindleparser.models.TocEntry;

public interface TocParser {

    public List<TocEntry> parse(String content) throws IOException;
}
