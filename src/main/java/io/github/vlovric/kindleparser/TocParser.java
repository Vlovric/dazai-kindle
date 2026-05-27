package io.github.vlovric.kindleparser;

import io.github.vlovric.kindleparser.models.TocEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the EPUB Table of Contents into a flattened list of TocEntry records.
 * Supports both modern EPUB 3 navigation files (nav.xhtml) and legacy EPUB 2 NCX files (toc.ncx).
 */
public class TocParser {

    private final EpubLoader loader;

    /**
     * Initializes the TocParser with an active EpubLoader instance.
     *
     * @param loader the loader referencing the unzipped EPUB contents
     */
    public TocParser(EpubLoader loader) {
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
        String html = new String(raw, StandardCharsets.UTF_8);

        if (tocHref.endsWith(".xhtml") || tocHref.endsWith(".html")) {
            return parseNav(html);
        }
        return parseNcx(html);
    }

    // ------------------------------------------------------------------
    // EPUB3 nav.xhtml
    // ------------------------------------------------------------------

    /**
     * Parses an EPUB 3 navigation document (typically nav.xhtml).
     * Extracts items sequentially from the primary nav[epub:type=toc] element.
     *
     * @param html the string content of the nav file
     * @return the extracted list of TocEntry records
     */
    private List<TocEntry> parseNav(String html) {
        Document soup = Jsoup.parse(html);
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

        String resolvedFile = filePart.isEmpty() ? "" : loader.resolve(filePart);
        return new TocEntry(a.text().trim(), resolvedFile, anchor, level);
    }

    // ------------------------------------------------------------------
    // EPUB2 toc.ncx
    // ------------------------------------------------------------------

    /**
     * Parses a legacy EPUB 2 NCX file containing the navigation map.
     * Starts extraction from the primary navMap element.
     *
     * @param xml the raw XML string of the NCX metadata
     * @return the extracted list of TocEntry records
     */
    private List<TocEntry> parseNcx(String xml) {
        Document soup = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
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

        String resolvedFile = filePart.isEmpty() ? "" : loader.resolve(filePart);
        return new TocEntry(textTag.text().trim(), resolvedFile, anchor, level);
    }
}
