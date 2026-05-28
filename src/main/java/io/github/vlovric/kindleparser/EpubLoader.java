package io.github.vlovric.kindleparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Responsible for opening an EPUB file, extracting it to a temporary directory (or specified directory for debugging),
 * and exposing its internal structure (OPF root, spine order, TOC location).
 */
public class EpubLoader implements AutoCloseable {

    /**
     * Represents a single file inside the EPUB spine.
     * The spine defines the sequential reading order of the book's content.
     * @param id The manifest ID
     * @param href The relative path to the file
     */
    public record SpineItem(String id, String href) {}

    private final Path epubPath;
    private final Path tempDir;
    private final boolean keepExtracted;
    private String opfRoot = "";
    private List<SpineItem> spine = new ArrayList<>();
    private String tocHref = "";
    private String bookTitle;
    private String bookAuthor;

    /**
     * Initializes the loader with the given EPUB path and creates a temp directory.
     *
     * @param epubPath the path to the EPUB file
     * @throws IOException if the temp directory cannot be created
     */
    public EpubLoader(Path epubPath) throws IOException {
        this.epubPath = epubPath;
        this.tempDir = Files.createTempDirectory("kindleparser_epub_");
        this.keepExtracted = false;
    }

    /**
     * Initializes the loader with the given EPUB path and extracts into the provided directory.
     * When keepExtracted is true, close() will not delete the directory.
     */
    public EpubLoader(Path epubPath, Path extractDir, boolean keepExtracted) throws IOException {
        this.epubPath = epubPath;
        this.tempDir = extractDir;
        this.keepExtracted = keepExtracted;
        Files.createDirectories(this.tempDir);
    }

    /**
     * Opens and extracts the EPUB, locating the OPF root, manifest, spine, and TOC.
     *
     * @throws IOException if reading or parsing the internal XML fails
     */
    public void open() throws IOException {
        // Extract the EPUB to a temporary directory
        extractZip(this.epubPath, this.tempDir);

        Path opfPath = findOpfPath();
        this.opfRoot = deriveOpfRoot(opfPath);

        parseMetadata(opfPath);

        Map<String, String> manifest = parseManifest(opfPath);
        this.spine = parseSpine(opfPath, manifest);
        this.tocHref = findTocHref(manifest);
    }

    private void parseMetadata(Path opfPath) {
        try {
            Document opf = Jsoup.parse(opfPath.toFile(), "UTF-8", "", org.jsoup.parser.Parser.xmlParser());
            Element metadata = opf.selectFirst("metadata");
            if (metadata == null) {
                return;
            }

            // Be namespace-tolerant: tagName might be "dc:title" or "title" depending on parsing.
            for (Element el : metadata.children()) {
                String tag = el.tagName();
                if (bookTitle == null && (tag.endsWith("title") || tag.equalsIgnoreCase("dc:title"))) {
                    String t = el.text();
                    if (t != null && !t.isBlank()) {
                        bookTitle = t.trim();
                    }
                }
                if (bookAuthor == null && (tag.endsWith("creator") || tag.equalsIgnoreCase("dc:creator"))) {
                    String a = el.text();
                    if (a != null && !a.isBlank()) {
                        bookAuthor = a.trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // Metadata is best-effort only.
        }
    }

    /**
     * Extracts the contents of the given zip archive to the output directory.
     *
     * @param zipPath the source zip/epub path
     * @param outDir  the destination temp directory
     * @throws IOException if extracting breaks
     */
    private void extractZip(Path zipPath, Path outDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryDest = outDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryDest);
                } else {
                    Files.createDirectories(entryDest.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryDest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    /**
     * Locates the OPF metadata file by reading META-INF/container.xml
     *
     * @return the absolute path to the OPF file
     * @throws IOException if the container XML is missing or unparseable
     */
    private Path findOpfPath() throws IOException {
        Path containerXml = tempDir.resolve("META-INF/container.xml");
        Document soup = Jsoup.parse(containerXml.toFile(), "UTF-8", "");
        Element rootfile = soup.selectFirst("rootfile");
        if (rootfile == null) {
            throw new IOException("Cannot find <rootfile> in META-INF/container.xml");
        }
        return tempDir.resolve(rootfile.attr("full-path"));
    }

    /**
     * Derives the logical root directory of the OPF relative to the EPUB structural root.
     *
     * @param opfPath the path to the OPF file
     * @return the relative parent directory of the OPF (e.g. "OEBPS"), or empty string if at root
     */
    private String deriveOpfRoot(Path opfPath) {
        Path relativeOpf = tempDir.relativize(opfPath);
        Path parent = relativeOpf.getParent();
        return parent == null ? "" : parent.toString();
    }

    /**
     * Parses the OPF file's manifest section.
     *
     * @param opfPath the path to the OPF file
     * @return a map of manifest item IDs to their corresponding href paths
     * @throws IOException if reading the OPF file fails
     */
    private Map<String, String> parseManifest(Path opfPath) throws IOException {
        Document opf = Jsoup.parse(opfPath.toFile(), "UTF-8", "");
        Map<String, String> manifest = new HashMap<>();
        
        for (Element item : opf.select("item")) {
            manifest.put(item.attr("id"), item.attr("href"));
        }
        return manifest;
    }

    /**
     * Parses the OPF file's spine section to determine the sequential reading order.
     *
     * @param opfPath   the path to the OPF file
     * @param manifest  the parsed manifest to resolve IDs to hrefs
     * @return an ordered list of SpineItems
     * @throws IOException if reading the OPF file fails
     */
    private List<SpineItem> parseSpine(Path opfPath, Map<String, String> manifest) throws IOException {
        Document opf = Jsoup.parse(opfPath.toFile(), "UTF-8", "");
        List<SpineItem> spineList = new ArrayList<>();
        
        Element spineNode = opf.selectFirst("spine");
        if (spineNode != null) {
            for (Element itemref : spineNode.select("itemref")) {
                String idref = itemref.attr("idref");
                if (manifest.containsKey(idref)) {
                    spineList.add(new SpineItem(idref, manifest.get(idref)));
                }
            }
        }
        return spineList;
    }

    /**
     * Determines the location of the Table of Contents file from the manifest.
     * Supports both EPUB 3 (nav.xhtml) and EPUB 2 (toc.ncx).
     *
     * @param manifest the parsed manifest
     * @return the href to the TOC file, or empty string if not found
     */
    private String findTocHref(Map<String, String> manifest) {
        // Find EPUB 3 toc
        for (String href : manifest.values()) {
            if (href.endsWith("nav.xhtml")) {
                return href; // simplified heuristic matching python logic
            }
        }
        // Find EPUB 2 toc
        if (manifest.containsKey("ncx")) {
            return manifest.get("ncx");
        }
        return "";
    }

    /**
     * Reads the contents of a file inside the unzipped EPUB.
     *
     * @param pathInsideEpub the relative path within the EPUB root
     * @return a byte array containing the file's data
     * @throws IOException if reading the file fails
     */
    public byte[] read(String pathInsideEpub) throws IOException {
        Path file = tempDir.resolve(pathInsideEpub);
        return Files.readAllBytes(file);
    }

    /**
     * Resolves a potentially relative href relative to the OPF root directory.
     *
     * @param href the relative href extracted from TOC or manifest
     * @return the fully qualified relative path inside the EPUB
     */
    public String resolve(String href) {
        String decoded = java.net.URLDecoder.decode(href, java.nio.charset.StandardCharsets.UTF_8);
        if (!opfRoot.isEmpty()) {
            return opfRoot + "/" + decoded;
        }
        return decoded;
    }

    /**
     * @return the resolved href linking to the Table of Contents file
     */
    public String getTocHref() { return tocHref; }

    /**
     * @return the sequential list of files representing the reading order
     */
    public List<SpineItem> getSpine() { return spine; }

    /**
     * @return the base directory holding the OPF file
     */
    public String getOpfRoot() { return opfRoot; }

    public Path getExtractDir() {
        return tempDir;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    /**
     * Cleans up the temporary directory containing the unzipped EPUB.
     *
     * @throws IOException if directory traversal or file deletion fails
     */
    @Override
    public void close() throws IOException {
        if (keepExtracted) {
            return;
        }
        if (Files.exists(tempDir)) {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
