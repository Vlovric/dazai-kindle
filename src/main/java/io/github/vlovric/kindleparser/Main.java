package io.github.vlovric.kindleparser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import io.github.vlovric.kindleparser.calibre.BookPreprocessor;
import io.github.vlovric.kindleparser.fyodor.FyodorClippingsParser;
import io.github.vlovric.kindleparser.fyodor.FyodorParseResult;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparser.toc.LocationResolver;
import io.github.vlovric.kindleparser.toc.TocParserResolver;

public class Main {

    @Option(name = "--book", required = true, usage = "Path to .epub or .azw3 file")
    private File book;

    @Option(name = "--clippings", usage = "Path to MyClippings.txt from your Kindle")
    private File clippings;

    @Option(name = "--title", usage = "Book title substring to filter clippings (case-insensitive)")
    private String title = "";

    @Option(name = "--template", usage = "Path to the custom Mustache template file for output format")
    private File template;

    @Option(name = "--output", usage = "Output file path. Defaults to <Book Title>.md if not specified")
    private File output;

    @Option(name = "--headings-only", usage = "Print extracted headings and their locations, then exit")
    private boolean headingsOnly = false;

    @Option(name = "--debug", usage = "Write intermediate artifacts to a run directory")
    private boolean debug = false;

    @Option(name = "--calibrate", usage = "Path to a calibration file containing lines like 'Heading - 123' or 'Heading: 123'. Applies calibration for this run only")
    private File calibrate;

    @Option(name = "--print-calibration-template", usage = "Write a calibration template containing all TOC entries to the specified file path, then exit")
    private File printCalibrationTemplate;

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

        boolean isPrintingCalibrationTemplate = printCalibrationTemplate != null;

        if (!isPrintingCalibrationTemplate && calibrate == null) {
            System.err.println("[KindleParser] ❌ Calibration is mandatory.");
            System.err.println("[KindleParser]    Provide --calibrate <file>, or run --print-calibration-template <path> to generate a template file.");
            System.exit(1);
        }

        if (!isPrintingCalibrationTemplate && calibrate != null && !calibrate.exists()) {
            System.err.println("[KindleParser] ❌ Calibration file not found: " + calibrate.getPath());
            System.exit(1);
        }

        if (!isPrintingCalibrationTemplate && !headingsOnly) {
            if (clippings == null) {
                System.err.println("[KindleParser] ❌ Missing --clippings.");
                System.exit(1);
            }
            if (template == null) {
                System.err.println("[KindleParser] ❌ Missing --template.");
                System.exit(1);
            }
        }

        try {
            boolean debugEnabled = debug;
            DebugArtifacts dbg = null;
            Path runDir = null;

            if (debugEnabled) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                runDir = Paths.get("./debug-runs", "run-" + ts);
                Files.createDirectories(runDir);
                
                dbg = new DebugArtifacts(runDir);
                System.out.println("[KindleParser] 🧪 Debug run dir: " + runDir.toAbsolutePath());
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

                if (isPrintingCalibrationTemplate) {
                    writeCalibrationTemplate(printCalibrationTemplate.toPath(), tocEntries);
                    return;
                }

                System.out.println("[KindleParser] 📍 Resolving locations for " + tocEntries.size() + " TOC entries...");
                LocationResolver resolver;

                CalibrationFit fit = fitCalibrationFromFile(calibrate.toPath(), loader, tocEntries);
                resolver = new LocationResolver(loader, fit.bytesPerLocation(), fit.locationBias());
                System.out.println("[KindleParser] 📐 Calibration fitted from file (bytesPerLocation="
                        + fit.bytesPerLocation() + ", bias=" + fit.locationBias() + ", points=" + fit.pointsUsed()
                        + ", rmse=" + String.format("%.2f", fit.rmseLocations()) + " locations)");

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

    private void writeCalibrationTemplate(Path outputPath, List<TocEntry> tocEntries) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# KindleParser calibration template\n");
        sb.append("# Fill in at least 2 locations, keep the rest blank.\n");
        sb.append("# Format: Title - 123\n\n");
        for (TocEntry e : tocEntries) {
            sb.append(e.title()).append(" - \n");
        }

        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("[KindleParser] ✅ Calibration template written to " + outputPath.toAbsolutePath());
    }

    private record CalibrationFit(double bytesPerLocation, double locationBias, int pointsUsed, double rmseLocations) {}

    private record CalibrationPoint(String wantedTitle, String matchedTitle, int wantedLocation, int byteOffset) {}

    private static CalibrationFit fitCalibrationFromFile(Path file, EpubLoader loader, List<TocEntry> tocEntries) throws IOException {
        Map<String, Integer> targets = parseCalibrationFile(file);
        if (targets.isEmpty()) {
            throw new IOException("Calibration file has no usable 'Heading - Location' entries: " + file);
        }

        LocationResolver uncalibrated = new LocationResolver(loader);

        // Build points: x = byteOffset, y = (kindleLocation - 1)
        List<Double> xs = new java.util.ArrayList<>();
        List<Double> ys = new java.util.ArrayList<>();
        List<CalibrationPoint> usedPoints = new java.util.ArrayList<>();

        int notFound = 0;
        int notResolvable = 0;

        for (Map.Entry<String, Integer> e : targets.entrySet()) {
            String wantedTitle = e.getKey();
            int wantedLoc = e.getValue();
            TocEntry match = findTocEntry(tocEntries, wantedTitle);
            if (match == null) {
                System.out.println("[KindleParser] ⚠️  Calibration title not found in TOC: '" + wantedTitle + "'");
                notFound++;
                continue;
            }
            Integer byteOffset = uncalibrated.byteOffsetOf(match);
            if (byteOffset == null) {
                System.out.println("[KindleParser] ⚠️  Calibration anchor/file not resolvable for: '" + match.title() + "'");
                notResolvable++;
                continue;
            }
            xs.add((double) byteOffset);
            ys.add((double) (wantedLoc - 1));
            usedPoints.add(new CalibrationPoint(wantedTitle, match.title(), wantedLoc, byteOffset));
        }

        System.out.println("[KindleParser] 📌 Calibration points: used=" + xs.size()
                + ", notFoundInToc=" + notFound
                + ", notResolvable=" + notResolvable);

        for (CalibrationPoint p : usedPoints) {
            System.out.println("[KindleParser]    '" + p.wantedTitle() + "' -> '" + p.matchedTitle()
                    + "' @ byteOffset=" + p.byteOffset() + " => location=" + p.wantedLocation());
        }

        if (xs.size() < 2) {
            throw new IOException("Need at least 2 matched calibration points; got " + xs.size());
        }

        // Least squares fit: y ≈ m*x + c
        int n = xs.size();
        double mx = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double my = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double num = 0;
        double den = 0;
        for (int i = 0; i < n; i++) {
            double dx = xs.get(i) - mx;
            double dy = ys.get(i) - my;
            num += dx * dy;
            den += dx * dx;
        }
        if (den == 0) {
            throw new IOException("Calibration points have zero variance in byte offsets (cannot fit)");
        }

        double m = num / den;
        if (m <= 0) {
            throw new IOException("Calibration fit produced non-positive slope (m=" + m + ")");
        }

        double c = my - (m * mx);
        double bytesPerLocation = 1.0 / m;
        double locationBias = c;

        // RMSE in locations on calibration points.
        double se = 0;
        for (int i = 0; i < n; i++) {
            double yHat = (m * xs.get(i)) + c;
            double err = ys.get(i) - yHat;
            se += err * err;
        }
        double rmse = Math.sqrt(se / n);

        return new CalibrationFit(bytesPerLocation, locationBias, xs.size(), rmse);
    }

    private static Map<String, Integer> parseCalibrationFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Map<String, Integer> out = new LinkedHashMap<>();

        // Supports:
        //   Heading - 123
        //   Heading: 123
        //   Heading — 123
        Pattern p = Pattern.compile("^\\s*(.+?)\\s*(?:-|:|—|–)\\s*(\\d+)\\s*$");

        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("#")) continue;

            Matcher m = p.matcher(trimmed);
            if (!m.matches()) {
                continue;
            }
            String title = m.group(1).trim();
            int loc = Integer.parseInt(m.group(2));
            if (!title.isEmpty() && loc > 0) {
                out.put(title, loc);
            }
        }
        return out;
    }

    private static TocEntry findTocEntry(List<TocEntry> tocEntries, String wantedTitle) {
        String wantedNorm = normalizeTitle(wantedTitle);
        if (wantedNorm.isEmpty()) {
            return null;
        }

        TocEntry exact = null;
        TocEntry prefix = null;
        TocEntry contains = null;

        for (TocEntry e : tocEntries) {
            String candNorm = normalizeTitle(e.title());
            if (candNorm.equals(wantedNorm)) {
                exact = e;
                break;
            }
            if (prefix == null && candNorm.startsWith(wantedNorm)) {
                prefix = e;
            }
            if (contains == null && candNorm.contains(wantedNorm)) {
                contains = e;
            }
        }

        if (exact != null) return exact;
        if (prefix != null) return prefix;
        return contains;
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase();
        // Replace any non-alphanumeric with spaces; collapse whitespace.
        String cleaned = lower.replaceAll("[^a-z0-9]+", " ").trim();
        return cleaned.replaceAll("\\s+", " ");
    }
}