package io.github.vlovric.kindleparser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
 * Resolves logical TOC entries to physical Kindle Locations.
 * Does so by calculating character offsets across all HTML texts in the spine,
 * mapping those distances mathematically based on Kindle's format assumption of 128 bytes/location.
 */
public class LocationResolver {

    private static final int KINDLE_BYTES_PER_LOCATION = 128;

    private final EpubLoader loader;
    private final Map<String, Integer> fileOffsets = new HashMap<>();
    private boolean offsetsBuilt = false;

    /**
     * Initializes the LocationResolver with the extracted EPUB context.
     *
     * @param loader the unzipped EPUB loader tool
     */
    public LocationResolver(EpubLoader loader) {
        this.loader = loader;
    }

    /**
     * Computes the Kindle locations for a list of table of contents entries.
     * Elements are sorted incrementally by calculated physical locations.
     *
     * @param entries raw TOC entries extracted by TocParser
     * @return contextualized Headings wrapping TOC references with physical mappings
     */
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

    /**
     * Confirms the internal file offset dictionary has been generated.
     */
    private void ensureOffsetsBuilt() {
        if (!offsetsBuilt) {
            buildFileOffsets();
            offsetsBuilt = true;
        }
    }

    /**
     * Debug helper: returns a snapshot of the computed per-file character offsets.
     */
    public Map<String, Integer> debugFileOffsets() {
        ensureOffsetsBuilt();
        return Collections.unmodifiableMap(new HashMap<>(fileOffsets));
    }

    /**
     * Processes every file progressively inside the EPUB spine in logical sequence.
     * Captures running totals of visible text character counts to calculate
     * physical base offsets across all files seamlessly.
     */
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

    /**
     * Resolves an individual TOC entry to its global logical offset across the book.
     * Evaluates fragment hashes against HTML anchors inside the specified DOM.
     *
     * @param entry the TOC reference
     * @return a qualified Heading detailing its global index/location, or null if unresolvable
     */
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

    /**
     * Finds the intra-document character offsets pointing directly to an anchor component.
     * Accomplishes this by mapping DOM text element accumulations prior to the identified DOM node.
     *
     * @param filePath   the relative path referencing the document body
     * @param anchor     the target `#id` node identifier
     * @param fileOffset the global character offset corresponding to the start of the document
     * @return the calculated global character offset, or null if the query string anchor cannot be parsed
     */
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

    /**
     * Reconstructs an HTML document directly from the resolved UTF-8 byte arrays,
     * stripping out inline scripts and style definitions.
     *
     * @param raw byte representation retrieved from the epub parser
     * @return cleaned Jsoup Document modeling the input HTML
     */
    private static Document parseContent(byte[] raw) {
        String html = new String(raw, StandardCharsets.UTF_8);
        Document soup = Jsoup.parse(html);
        soup.select("script, style").remove();
        return soup;
    }

    /**
     * Traverses the specified resource extracting concatenated raw text blocks 
     * linearly to facilitate byte offset calculations.
     *
     * @param filePath the absolute path pointing within the EPUB spine files
     * @return complete text concatenation representing document length
     */
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

    /**
     * Recursively traverses DOM text nodes calculating cumulative total characters
     * up-to and ceasing immediately once traversing over the Target marker.
     *
     * @param soup   the parent Document scope parsing text components
     * @param target the objective Node marking the destination boundary
     * @return the cumulative string length measured sequentially
     */
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

    /**
     * Converts raw byte offsets into physical Kindle Locations.
     * Uses assumption that one Kindle Location typically covers 128 bytes.
     *
     * @param charOffset the byte index mapping globally across the book elements
     * @return the calculated location metric as perceived by Amazon Kindle firmware
     */
    private static int toKindleLocation(int charOffset) {
        return (charOffset / KINDLE_BYTES_PER_LOCATION) + 1;
    }
}
