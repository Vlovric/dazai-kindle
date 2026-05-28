package io.github.vlovric.kindleparser.toc;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.github.vlovric.kindleparser.models.TocEntry;

public class NcxTocParser implements TocParser {

    @Override
    /**
     * Parses a legacy EPUB 2 NCX file containing the navigation map.
     * Starts extraction from the primary navMap element.
     *
     * @param xml the raw XML string of the NCX metadata
     * @return the extracted list of TocEntry records
     */
    public List<TocEntry> parse(String content) throws IOException {
        Document soup = Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
        List<TocEntry> entries = new ArrayList<>();
        Element navMap = soup.selectFirst("navMap");
        if (navMap != null) {
            walkNcxNavMap(navMap, entries, 1);
        }
        return entries;
    }

    /**
     * Recursively walks NCX navPoint hierarchies to build TocEntries.
     *
     * @param node    the parent navMap or navPoint XML element
     * @param entries the accumulator list
     * @param level   the current depth
     */
    private void walkNcxNavMap(Element node, List<TocEntry> entries, int level) {
        for (Element navPoint : node.children()) {
            if ("navPoint".equalsIgnoreCase(navPoint.tagName())) {
                TocEntry entry = ncxNavPointToEntry(navPoint, level);
                if (entry != null) {
                    entries.add(entry);
                }
                walkNcxNavMap(navPoint, entries, level + 1);
            }
        }
    }

    /**
     * Converts a single NCX navPoint XML block into a structured TocEntry.
     * Evaluates relative src paths and decomposes hash links.
     *
     * @param navPoint the XML wrapper tag
     * @param level    the hierarchy depth
     * @return a mapped TocEntry, or null if essential labels/content are missing
     */
    private TocEntry ncxNavPointToEntry(Element navPoint, int level) {
        Element textTag = navPoint.selectFirst("navLabel > text"); // ncx usually has navLabel containing text
        if (textTag == null) textTag = navPoint.selectFirst("text"); // fallback to direct text

        Element contentTag = navPoint.selectFirst("content");
        
        if (textTag == null || contentTag == null) {
            return null;
        }

        String rawSrc = URLDecoder.decode(contentTag.attr("src"), StandardCharsets.UTF_8);
        String filePart = rawSrc;
        String anchor = null;
        
        int hashIdx = rawSrc.indexOf('#');
        if (hashIdx != -1) {
            filePart = rawSrc.substring(0, hashIdx);
            anchor = rawSrc.substring(hashIdx + 1);
        }

        return new TocEntry(textTag.text().trim(), filePart, anchor, level);
    }
    
}
