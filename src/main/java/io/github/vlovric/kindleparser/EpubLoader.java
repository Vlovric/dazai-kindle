package io.github.vlovric.kindleparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
 * Handles extracting the EPUB file locally into a Files.createTempDirectory
 * It parses the internal XML tags from  META-INF/container.xml mapping the OPF
 * and correctly extracting the Spine dependencies and checking for the EPUB2 or EPUB3 TOC entry location
 * It implements AutoCloseable directly, so it automatically tears down the temp directory recursively when it finishes.
 */
public class EpubLoader implements AutoCloseable {

    public record SpineItem(String id, String href) {}

    private final Path epubPath;
    private final Path tempDir;
    private String opfRoot = "";
    private List<SpineItem> spine = new ArrayList<>();
    private String tocHref = "";

    public EpubLoader(Path epubPath) throws IOException {
        this.epubPath = epubPath;
        this.tempDir = Files.createTempDirectory("kindleparser_epub_");
    }

    public void open() throws IOException {
        // Extract the EPUB to a temporary directory
        extractZip(this.epubPath, this.tempDir);

        Path opfPath = findOpfPath();
        this.opfRoot = deriveOpfRoot(opfPath);

        Map<String, String> manifest = parseManifest(opfPath);
        this.spine = parseSpine(opfPath, manifest);
        this.tocHref = findTocHref(manifest);
    }

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

    private Path findOpfPath() throws IOException {
        Path containerXml = tempDir.resolve("META-INF/container.xml");
        Document soup = Jsoup.parse(containerXml.toFile(), "UTF-8", "");
        Element rootfile = soup.selectFirst("rootfile");
        if (rootfile == null) {
            throw new IOException("Cannot find <rootfile> in META-INF/container.xml");
        }
        return tempDir.resolve(rootfile.attr("full-path"));
    }

    private String deriveOpfRoot(Path opfPath) {
        Path relativeOpf = tempDir.relativize(opfPath);
        Path parent = relativeOpf.getParent();
        return parent == null ? "" : parent.toString();
    }

    private Map<String, String> parseManifest(Path opfPath) throws IOException {
        Document opf = Jsoup.parse(opfPath.toFile(), "UTF-8", "");
        Map<String, String> manifest = new HashMap<>();
        
        for (Element item : opf.select("item")) {
            manifest.put(item.attr("id"), item.attr("href"));
        }
        return manifest;
    }

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

    private String findTocHref(Map<String, String> manifest) {
        // Find EPUB 3 toc
        for (String href : manifest.values()) {
            if (href.endsWith("nav.xhtml")) {
                return href;
            }
        }
        // Find EPUB 2 toc
        if (manifest.containsKey("ncx")) {
            return manifest.get("ncx");
        }
        return "";
    }

    public byte[] read(String pathInsideEpub) throws IOException {
        Path file = tempDir.resolve(pathInsideEpub);
        return Files.readAllBytes(file);
    }

    public String resolve(String href) {
        String decoded = java.net.URLDecoder.decode(href, java.nio.charset.StandardCharsets.UTF_8);
        if (!opfRoot.isEmpty()) {
            return opfRoot + "/" + decoded;
        }
        return decoded;
    }

    public String getTocHref() { return tocHref; }
    public List<SpineItem> getSpine() { return spine; }
    public String getOpfRoot() { return opfRoot; }

    @Override
    public void close() throws IOException {
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
