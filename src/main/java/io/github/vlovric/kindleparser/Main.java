package io.github.vlovric.kindleparser;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.models.TocEntry;

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
            Path bookPath = book.toPath();
            System.out.println("[KindleParser] Checking input file...");
            bookPath = BookPreprocessor.preprocess(bookPath);

            System.out.println("[KindleParser] 📖 Loaded: " + bookPath);
            
            List<Heading> resolvedHeadings;
            try (EpubLoader loader = new EpubLoader(bookPath)) {
                loader.open();
                
                System.out.println("[KindleParser] 📑 Parsing table of contents...");
                TocParser tocParser = new TocParser(loader);
                List<TocEntry> tocEntries = tocParser.parse();
                
                if (tocEntries.isEmpty()) {
                    System.err.println("[KindleParser] ⚠️  No TOC entries found. Is there a toc.ncx or nav.xhtml?");
                    System.exit(1);
                }

                System.out.println("[KindleParser] 📍 Resolving locations for " + tocEntries.size() + " TOC entries...");
                LocationResolver resolver = new LocationResolver(loader);
                resolvedHeadings = resolver.resolve(tocEntries);
            }

            if (headingsOnly) {
                printHeadingsTable(resolvedHeadings);
                return;
            }

            System.out.println("[KindleParser] ✂️  Parsing clippings via Fyodor Subprocess: " + clippings.getPath());
            FyodorClippingsParser clippingsParser = new FyodorClippingsParser(clippings.toPath());
            List<Clipping> parsedClippings = clippingsParser.parse(title);

            if (parsedClippings.isEmpty()) {
                String suffix = !title.isEmpty() ? " matching '" + title + "'" : "";
                System.out.println("[KindleParser] ⚠️  No clippings found" + suffix + ".");
                System.exit(1);
            }

            String matchedBookName = parsedClippings.get(0).bookTitle();
            System.out.println("[KindleParser]    Found " + parsedClippings.size() + " clipping(s) for '" + matchedBookName + "'");

            System.out.println("[KindleParser] 🗂️  Grouping under headings...");
            Grouper grouper = new Grouper();
            List<HeadingGroup> groups = grouper.group(parsedClippings, resolvedHeadings);

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