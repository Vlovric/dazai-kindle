package io.github.vlovric.kindleparser;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.github.vlovric.kindleparser.models.TocEntry;

/**
 * Maps perfectly to the toc_parser.py hierarchy
 * It checks if the TOC link ends in .html or .xhtml, utilizing Jsoup to traverse the EPUB3 <nav> nodes
 * or correctly handles legacy EPUB2 .ncx files parsing <navMap>
 * It resolves everything sequentially into a standard List<TocEntry>
 */
public class TocParser {

    private final EpubLoader loader;

    public TocParser(EpubLoader loader) {
        this.loader = loader;
    }

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

    private List<TocEntry> parseNcx(String xml) {
        Document soup = Jsoup.parse(xml, "", org.jsoup.parser.Parser.xmlParser());
        List<TocEntry> entries = new ArrayList<>();
        Element navMap = soup.selectFirst("navMap");
        if (navMap != null) {
            walkNcxNavMap(navMap, entries, 1);
        }
        return entries;
    }

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
