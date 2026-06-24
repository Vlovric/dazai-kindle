package io.github.vlovric.dazaikindle.toc;

import java.io.IOException;
import java.util.List;

import io.github.vlovric.dazaikindle.models.TocEntry;

public interface TocParser {

    public List<TocEntry> parse(String content) throws IOException;
}
