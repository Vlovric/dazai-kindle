package io.github.vlovric.dazaikindle.toc;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.github.vlovric.dazaikindle.models.TocEntry;

public class XhtmlTocParser implements TocParser {

    @Override
    /**
     * Parses an EPUB 3 navigation document (typically nav.xhtml).
     * Extracts items sequentially from the primary nav[epub:type=toc] element.
     *
     * @param content the string content of the nav file
     * @return the extracted list of TocEntry records
     */
    public List<TocEntry> parse(String content) throws IOException {
        Document soup = Jsoup.parse(content);
        // Look for <nav epub:type="toc"> or fallback to first <nav>
        Element nav = soup.selectFirst("nav[epub\\:type=toc]");
        if (nav == null) {
            nav = soup.selectFirst("nav");
        }
        if (nav == null) {
            return new ArrayList<>();
        }

        List<TocEntry> entries = new ArrayList<>();
        Element ol = nav.selectFirst("ol");
        if (ol != null) {
            walkNavOl(ol, entries, 1);
        }
        return entries;
    }

    /**
     * Recursively walks unordered/ordered lists in nav files to build nested TocEntries.
     *
     * @param ol       the parent list element
     * @param entries  the accumulator for storing discovered TocEntries
     * @param level    the current heading depth
     */
    private void walkNavOl(Element ol, List<TocEntry> entries, int level) {
        for (Element li : ol.children()) {
            if ("li".equalsIgnoreCase(li.tagName())) {
                Element a = li.selectFirst("a");
                if (a != null && a.hasAttr("href")) {
                    entries.add(navAnchorToEntry(a, level));
                }
                
                Element nestedOl = li.selectFirst("ol");
                if (nestedOl != null) {
                    walkNavOl(nestedOl, entries, level + 1);
                }
            }
        }
    }

    /**
     * Converts a Jsoup link element into a parsed TocEntry.
     * Extracts the href and cleans up hash anchors.
     *
     * @param a     the anchor HTML element
     * @param level the calculated heading depth
     * @return a mapped TocEntry record
     */
    private TocEntry navAnchorToEntry(Element a, int level) {
        String rawHref = URLDecoder.decode(a.attr("href"), StandardCharsets.UTF_8);
        String filePart = rawHref;
        String anchor = null;
        
        int hashIdx = rawHref.indexOf('#');
        if (hashIdx != -1) {
            filePart = rawHref.substring(0, hashIdx);
            anchor = rawHref.substring(hashIdx + 1);
        }

        return new TocEntry(a.text().trim(), filePart, anchor, level);
    }
    
}
