package io.github.vlovric.kindleparser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.TocEntry;

/**
 * Iterates over the spine content, traverses the HTML DOM (stripping out script and style tags)
 * Accurately tracks character counts to mimic the Kindle's raw byte location calculation (roughly 128 chars per internal location).
 */
public class LocationResolver {

    private static final int KINDLE_BYTES_PER_LOCATION = 128;

    private final EpubLoader loader;
    private final Map<String, Integer> fileOffsets = new HashMap<>();
    private boolean offsetsBuilt = false;

    public LocationResolver(EpubLoader loader) {
        this.loader = loader;
    }

    public List<Heading> resolve(List<TocEntry> entries) {
        ensureOffsetsBuilt();
        List<Heading> headings = new ArrayList<>();
        for (TocEntry entry : entries) {
            Heading heading = resolveEntry(entry);
            if (heading != null) {
                headings.add(heading);
            }
        }
        headings.sort(Comparator.comparingInt(Heading::location));
        return headings;
    }

    private void ensureOffsetsBuilt() {
        if (!offsetsBuilt) {
            buildFileOffsets();
            offsetsBuilt = true;
        }
    }

    private void buildFileOffsets() {
        int cursor = 0;
        for (EpubLoader.SpineItem item : loader.getSpine()) {
            String fullPath = loader.resolve(item.href());
            String text = extractText(fullPath);
            if (text != null) {
                fileOffsets.put(fullPath, cursor);
                cursor += text.length();
            }
        }
    }

    private Heading resolveEntry(TocEntry entry) {
        Integer fileOffset = fileOffsets.get(entry.file());
        if (fileOffset == null) {
            return null;
        }

        int charOffset = fileOffset;
        if (entry.anchor() != null) {
            Integer anchorOff = anchorOffset(entry.file(), entry.anchor(), fileOffset);
            if (anchorOff != null) {
                charOffset = anchorOff;
            }
        }

        return new Heading(entry, charOffset, toKindleLocation(charOffset));
    }

    private Integer anchorOffset(String filePath, String anchor, int fileOffset) {
        try {
            byte[] raw = loader.read(filePath);
            Document soup = parseContent(raw);
            Element target = soup.getElementById(anchor);
            if (target == null) {
                return null;
            }

            String preText = textBefore(soup, target);
            return fileOffset + preText.length();
        } catch (Exception e) {
            return null;
        }
    }

    private static Document parseContent(byte[] raw) {
        String html = new String(raw, StandardCharsets.UTF_8);
        Document soup = Jsoup.parse(html);
        soup.select("script, style").remove();
        return soup;
    }

    private String extractText(String filePath) {
        try {
            byte[] raw = loader.read(filePath);
            Document soup = parseContent(raw);
            StringBuilder sb = new StringBuilder();
            NodeTraversor.traverse(new NodeVisitor() {
                @Override
                public void head(Node node, int depth) {
                    if (node instanceof TextNode textNode) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(textNode.getWholeText());
                    }
                }
                @Override
                public void tail(Node node, int depth) {}
            }, soup);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String textBefore(Document soup, Element target) {
        StringBuilder sb = new StringBuilder();
        class Visitor implements NodeVisitor {
            boolean found = false;

            @Override
            public void head(Node node, int depth) {
                if (found) return;
                if (node == target) {
                    found = true;
                    return;
                }
                if (node instanceof TextNode textNode) {
                    sb.append(textNode.getWholeText());
                }
            }

            @Override
            public void tail(Node node, int depth) {}
        }
        NodeTraversor.traverse(new Visitor(), soup);
        return sb.toString();
    }

    private static int toKindleLocation(int charOffset) {
        return (charOffset / KINDLE_BYTES_PER_LOCATION) + 1;
    }
}
