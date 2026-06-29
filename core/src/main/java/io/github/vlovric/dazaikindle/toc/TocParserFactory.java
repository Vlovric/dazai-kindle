package io.github.vlovric.dazaikindle.toc;

public class TocParserFactory {
    public static TocParser forFile(String filename) {
        if (filename.endsWith(".xhtml") || filename.endsWith(".html")) {
            return new XhtmlTocParser();
        }
        if (filename.endsWith(".ncx")) {
            return new NcxTocParser();
        }
        throw new IllegalArgumentException("Unknown TOC format: " + filename);
    }
}
