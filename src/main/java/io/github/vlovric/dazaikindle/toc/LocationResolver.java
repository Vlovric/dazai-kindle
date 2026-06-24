package io.github.vlovric.dazaikindle.toc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.vlovric.dazaikindle.EpubLoader;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.TocEntry;

/**
 * Resolves logical TOC entries to physical Kindle Locations.
 * Does so by calculating byte offsets across all spine XHTML resources,
 * mapping those distances mathematically based on Kindle's format assumption of 128 bytes/location.
 */
public class LocationResolver {

    private static final double DEFAULT_BYTES_PER_LOCATION = 128.0;
    private static final double DEFAULT_LOCATION_BIAS = 0.0;

    private final EpubLoader loader;
    private final double bytesPerLocation;
    private final double locationBias;
    private final Map<String, Integer> fileOffsets = new HashMap<>();
    private boolean offsetsBuilt = false;

    /**
     * Initializes the LocationResolver with the extracted EPUB context.
     *
     * @param loader the unzipped EPUB loader tool
     */
    public LocationResolver(EpubLoader loader) {
        this.loader = loader;
        this.bytesPerLocation = DEFAULT_BYTES_PER_LOCATION;
        this.locationBias = DEFAULT_LOCATION_BIAS;
    }

    /**
     * Initializes the resolver with an optional linear calibration:
     * location ≈ floor((byteOffset / bytesPerLocation) + locationBias) + 1
     */
    public LocationResolver(EpubLoader loader, double bytesPerLocation, double locationBias) {
        this.loader = loader;
        this.bytesPerLocation = bytesPerLocation > 0 ? bytesPerLocation : DEFAULT_BYTES_PER_LOCATION;
        this.locationBias = locationBias;
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
     * Resolves a TOC entry to its global byte offset across the spine.
     * Returns null if the entry file is not part of the spine offsets or the anchor cannot be located.
     */
    public Integer byteOffsetOf(TocEntry entry) {
        ensureOffsetsBuilt();
        Integer fileOffset = fileOffsets.get(entry.file());
        if (fileOffset == null) {
            return null;
        }

        int byteOffset = fileOffset;
        if (entry.anchor() != null) {
            Integer anchorOff = anchorOffset(entry.file(), entry.anchor(), fileOffset);
            if (anchorOff == null) {
                return null;
            }
            byteOffset = anchorOff;
        }

        return byteOffset;
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
     * Debug helper: returns a snapshot of the computed per-file byte offsets.
     */
    public Map<String, Integer> debugFileOffsets() {
        ensureOffsetsBuilt();
        return Collections.unmodifiableMap(new HashMap<>(fileOffsets));
    }

    /**
     * Processes every file progressively inside the EPUB spine in logical sequence.
     * Captures running totals of raw UTF-8 byte counts to calculate
     * physical base offsets across all files seamlessly.
     */
    private void buildFileOffsets() {
        int cursor = 0;
        for (EpubLoader.SpineItem item : loader.getSpine()) {
            String fullPath = loader.resolve(item.href());
            try {
                byte[] raw = loader.read(fullPath);
                fileOffsets.put(fullPath, cursor);
                cursor += raw.length;
            } catch (Exception ignored) {
                // Skip unreadable spine items.
            }
        }
    }

    /**
     * Resolves an individual TOC entry to its global logical offset across the book.
        * Evaluates fragment hashes against HTML anchors inside the resolved resource.
     *
     * @param entry the TOC reference
     * @return a qualified Heading detailing its global index/location, or null if unresolvable
     */
    private Heading resolveEntry(TocEntry entry) {
        Integer fileOffset = fileOffsets.get(entry.file());
        if (fileOffset == null) {
            return null;
        }

        int byteOffset = fileOffset;
        if (entry.anchor() != null) {
            Integer anchorOff = anchorOffset(entry.file(), entry.anchor(), fileOffset);
            if (anchorOff != null) {
                byteOffset = anchorOff;
            }
        }

        return new Heading(entry, byteOffset, toKindleLocation(byteOffset));
    }

    /**
     * Finds the intra-document byte offsets pointing directly to an anchor component.
     * Uses the raw XHTML/HTML representation and locates the anchor attribute (id/name/xml:id).
     *
     * @param filePath   the relative path referencing the document body
     * @param anchor     the target `#id` node identifier
     * @param fileOffset the global byte offset corresponding to the start of the document
     * @return the calculated global byte offset, or null if the query string anchor cannot be parsed
     */
    private Integer anchorOffset(String filePath, String anchor, int fileOffset) {
        try {
            byte[] raw = loader.read(filePath);
            String html = new String(raw, StandardCharsets.UTF_8);
            Integer localByteOffset = findAnchorByteOffset(html, anchor);
            if (localByteOffset == null) {
                return null;
            }
            return fileOffset + localByteOffset;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer findAnchorByteOffset(String html, String anchor) {
        int pos = findAnchorTokenStart(html, anchor);
        if (pos < 0) {
            return null;
        }

        // Approximate the start of the element (rather than the attribute itself).
        int tagStart = html.lastIndexOf('<', pos);
        if (tagStart >= 0) {
            pos = tagStart;
        }

        return html.substring(0, pos).getBytes(StandardCharsets.UTF_8).length;
    }

    private static int findAnchorTokenStart(String html, String anchor) {
        // Typical patterns:
        //   <a id="chapter1.3"></a>
        //   <h2 xml:id='intro1'>...
        //   <a name="foo"></a>
        String quoted = "\\b(?:id|name|xml:id)\\s*=\\s*(['\"])" + Pattern.quote(anchor) + "\\1";
        Matcher m = Pattern.compile(quoted, Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) {
            return m.start();
        }

        // Fallback: tolerate unquoted attributes (rare, but harmless to support).
        String unquoted = "\\b(?:id|name|xml:id)\\s*=\\s*" + Pattern.quote(anchor) + "\\b";
        m = Pattern.compile(unquoted, Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) {
            return m.start();
        }

        return -1;
    }

    /**
     * Converts raw byte offsets into physical Kindle Locations.
     * Uses assumption that one Kindle Location typically covers 128 bytes.
     *
     * @param charOffset the byte index mapping globally across the book elements
     * @return the calculated location metric as perceived by Amazon Kindle firmware
     */
    private int toKindleLocation(int charOffset) {
        double loc = (charOffset / bytesPerLocation) + locationBias;
        return ((int) Math.floor(loc)) + 1;
    }
}
