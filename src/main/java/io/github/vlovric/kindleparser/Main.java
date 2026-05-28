package io.github.vlovric.kindleparser;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparser.toc.TocParserResolver;

public class Main {

    @Option(name = "--book", required = true, usage = "Path to .epub or .azw3 file")
    private File book;

    @Option(name = "--clippings", required = true, usage = "Path to MyClippings.txt from your Kindle")
    private File clippings;

    @Option(name = "--title", usage = "Book title substring to filter clippings (case-insensitive)")
    private String title = "";

    @Option(name = "--template", required = true, usage = "Path to the custom Mustache template file for output format")
    private File template;

    @Option(name = "--output", usage = "Output file path. Defaults to <Book Title>.md if not specified")
    private File output;

    @Option(name = "--headings-only", usage = "Print extracted headings and their locations, then exit")
    private boolean headingsOnly = false;

    @Option(name = "--debug", usage = "Write intermediate artifacts to a run directory")
    private boolean debug = false;

    @Option(name = "--debug-dir", usage = "Directory for debug runs (default: ./debug-runs)")
    private File debugDir = new File("./debug-runs");

    @Option(name = "--workdir", usage = "Explicit work directory to reuse for debugging")
    private File workdir;

    @Option(name = "--keep-workdir", usage = "Keep intermediate directories (implied by --debug)")
    private boolean keepWorkdir = false;

    public static void main(String[] args) {
        new Main().run(args);
    }

    private void run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java -jar kindle-parser.jar [options...]");
            parser.printUsage(System.err);
            System.exit(1);
        }

        try {
            boolean debugEnabled = debug || workdir != null;
            DebugArtifacts dbg = null;
            Path runDir = null;

            if (debugEnabled) {
                keepWorkdir = true;
                if (workdir != null) {
                    runDir = workdir.toPath();
                } else {
                    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                    runDir = debugDir.toPath().resolve("run-" + ts);
                }
                Files.createDirectories(runDir);
                dbg = new DebugArtifacts(runDir);
                System.out.println("[KindleParser] 🧪 Debug run dir: " + runDir.toAbsolutePath());

                Map<String, Object> argDump = new HashMap<>();
                argDump.put("book", book == null ? null : book.getPath());
                argDump.put("clippings", clippings == null ? null : clippings.getPath());
                argDump.put("title", title);
                argDump.put("template", template == null ? null : template.getPath());
                argDump.put("output", output == null ? null : output.getPath());
                argDump.put("headingsOnly", headingsOnly);
                dbg.writeJson("00_run_args.json", argDump);
            }

            Path bookPath = book.toPath();
            System.out.println("[KindleParser] Checking input file...");
            bookPath = BookPreprocessor.preprocess(bookPath);

            System.out.println("[KindleParser] 📖 Loaded: " + bookPath);
            
            List<Heading> resolvedHeadings;
            String epubTitle = null;
            String epubAuthor = null;

            try (EpubLoader loader = debugEnabled
                    ? new EpubLoader(bookPath, runDir.resolve("01_epub_extracted"), true)
                    : new EpubLoader(bookPath)) {
                loader.open();

                epubTitle = loader.getBookTitle();
                epubAuthor = loader.getBookAuthor();

                if (dbg != null) {
                    dbg.writeJson("01_epub_metadata.json", Map.of(
                            "epubPath", bookPath.toAbsolutePath().toString(),
                            "extractDir", loader.getExtractDir().toAbsolutePath().toString(),
                            "opfRoot", loader.getOpfRoot(),
                            "tocHref", loader.getTocHref(),
                            "title", epubTitle,
                            "author", epubAuthor
                    ));
                    dbg.writeJson("01_spine.json", loader.getSpine());
                }
                
                System.out.println("[KindleParser] 📑 Parsing table of contents...");
                TocParserResolver tocResolver = new TocParserResolver(loader);
                List<TocEntry> tocEntries = tocResolver.parse();

                if (dbg != null) {
                    dbg.writeJson("02_toc_entries.json", tocEntries);
                    dbg.writeText("02_toc_source.txt", "tocHref=" + loader.getTocHref() + "\nresolved=" + loader.resolve(loader.getTocHref()) + "\n");
                }
                
                if (tocEntries.isEmpty()) {
                    System.err.println("[KindleParser] ⚠️  No TOC entries found. Is there a toc.ncx or nav.xhtml?");
                    System.exit(1);
                }

                System.out.println("[KindleParser] 📍 Resolving locations for " + tocEntries.size() + " TOC entries...");
                LocationResolver resolver = new LocationResolver(loader);
                resolvedHeadings = resolver.resolve(tocEntries);

                if (dbg != null) {
                    dbg.writeJson("03_resolved_headings.json", resolvedHeadings);
                    dbg.writeJson("03_file_offsets.json", resolver.debugFileOffsets());
                }
            }

            if (headingsOnly) {
                printHeadingsTable(resolvedHeadings);
                return;
            }

            System.out.println("[KindleParser] ✂️  Parsing clippings via Fyodor Subprocess: " + clippings.getPath());
            FyodorClippingsParser clippingsParser = new FyodorClippingsParser(clippings.toPath());
            Path fyodorOutDir = dbg != null ? dbg.runDir().resolve("fyodor-out") : null;
            FyodorParseResult fyodorResult = clippingsParser.parse(title, epubTitle, fyodorOutDir, dbg);
            List<Clipping> parsedClippings = fyodorResult.clippings();

            if (dbg != null) {
                Map<String, Integer> typeCounts = new HashMap<>();
                int nullLoc = 0;
                Integer minLoc = null;
                Integer maxLoc = null;

                for (Clipping c : parsedClippings) {
                    String t = c.type() == null ? "(null)" : c.type();
                    typeCounts.put(t, typeCounts.getOrDefault(t, 0) + 1);
                    Integer loc = c.location();
                    if (loc == null) {
                        nullLoc++;
                    } else {
                        minLoc = (minLoc == null) ? loc : Math.min(minLoc, loc);
                        maxLoc = (maxLoc == null) ? loc : Math.max(maxLoc, loc);
                    }
                }

                dbg.writeJson("05_clippings_stats.json", Map.of(
                        "selectedFile", fyodorResult.selectedFile() == null ? null : fyodorResult.selectedFile().toAbsolutePath().toString(),
                        "selectedBookTitle", fyodorResult.selectedBookTitle(),
                        "expectedEpubTitle", epubTitle,
                        "count", parsedClippings.size(),
                        "typeCounts", typeCounts,
                        "nullLocationCount", nullLoc,
                        "minLocation", minLoc,
                        "maxLocation", maxLoc
                ));

                dbg.writeJson("05_clippings_sample.json", parsedClippings.stream().limit(25).toList());
            }

            if (parsedClippings.isEmpty()) {
                String suffix = !title.isEmpty() ? " matching '" + title + "'" : "";
                System.out.println("[KindleParser] ⚠️  No clippings found" + suffix + ".");
                System.exit(1);
            }

            String matchedBookName = fyodorResult.selectedBookTitle();
            if (matchedBookName == null || matchedBookName.isBlank()) {
                matchedBookName = parsedClippings.get(0).bookTitle();
            }
            System.out.println("[KindleParser]    Found " + parsedClippings.size() + " clipping(s) for '" + matchedBookName + "'");

            System.out.println("[KindleParser] 🗂️  Grouping under headings...");
            Grouper grouper = new Grouper();
            List<HeadingGroup> groups = grouper.group(parsedClippings, resolvedHeadings);

            if (dbg != null) {
                List<Map<String, Object>> summary = groups.stream().map(g -> Map.<String, Object>of(
                    "headingTitle", g.heading().title(),
                    "headingLocation", g.heading().location(),
                    "level", g.heading().level(),
                    "clippingCount", g.clippings().size()
                )).toList();
                dbg.writeJson("06_grouping_summary.json", summary);

                HeadingGroup beforeFirst = groups.stream().filter(g -> g.heading().title().equals("(Before first heading)")).findFirst().orElse(null);
                if (beforeFirst != null) {
                    dbg.writeJson("06_before_first_bucket.json", beforeFirst.clippings());
                }
            }

            String finalTitle = (matchedBookName != null && !matchedBookName.isEmpty()) ? matchedBookName : title;
            
            File finalOutput = output;
            if (finalOutput == null) {
                String baseName = finalTitle;
                if (baseName == null || baseName.isBlank()) {
                    String bookName = bookPath.getFileName().toString();
                    int dotIndex = bookName.lastIndexOf('.');
                    baseName = (dotIndex == -1) ? bookName : bookName.substring(0, dotIndex);
                }
                baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
                finalOutput = new File(baseName + ".md");
            }

            TemplateRenderer renderer = new TemplateRenderer(template.toPath());
            try (FileWriter writer = new FileWriter(finalOutput)) {
                renderer.render(groups, finalTitle, writer);
                System.out.println("\n[KindleParser] ✅ Saved to " + finalOutput.getPath());
                System.out.println("[KindleParser] ✅ Done.");
            }

        } catch (Exception e) {
            System.err.println("[KindleParser] ❌ Pipeline Error:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void printHeadingsTable(List<Heading> headings) {
        String divider = "─".repeat(60);
        System.out.println("\n" + divider);
        System.out.printf("%-12s %-8s %s%n", "LOCATION", "LEVEL", "TITLE");
        System.out.println(divider);
        for (Heading h : headings) {
            String indent = "  ".repeat(Math.max(0, h.level() - 1));
            System.out.printf("%-12d h%-7d %s%s%n", h.location(), h.level(), indent, h.title());
        }
    }
}