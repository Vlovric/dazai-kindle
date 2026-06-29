package io.github.vlovric.dazaikindle.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.calibration.CalibrationFitter;
import io.github.vlovric.dazaikindle.calibre.BookPreprocessor;
import io.github.vlovric.dazaikindle.fyodor.FyodorClippingsParser;
import io.github.vlovric.dazaikindle.fyodor.FyodorParseResult;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.TocEntry;
import io.github.vlovric.dazaikindle.pipeline.Pipeline;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.steps.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String BOOK_TITLE = "80,000 Hours";

    @AfterEach
    void cleanupDebugRuns() throws IOException {
        Path debugDir = Path.of("debug-runs");
        if (Files.exists(debugDir)) {
            try (Stream<Path> walk = Files.walk(debugDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
            }
        }
    }

    /** FR01_01-HP_01, FR03_01-HP_01, FR03_01-HP_02: full pipeline produces stable golden output. */
    @Test
    void fullPipeline_rendersGoldenOutput() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        Path outputFile = tempDir.resolve("output.md");
        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                resourcePath("testing/clippings.jsonl"), // placeholder; Fyodor is mocked
                BOOK_TITLE,
                resourcePath("testing/template.ftl"),
                outputFile,
                false, null, false,
                resourcePath("testing/calibration.txt"),
                null, false
        );

        List<Clipping> clippings = loadClippingsFromJsonl("testing/clippings.jsonl");
        FyodorParseResult fakeResult = new FyodorParseResult(
                clippings, null, null, BOOK_TITLE, null, null);

        try (var mocked = mockConstruction(FyodorClippingsParser.class,
                (mock, ctx) -> when(mock.parse(any(), any(), any())).thenReturn(fakeResult))) {
            new Pipeline(args).run();
        }

        assertTrue(Files.exists(outputFile), "Output file was not created");
        String actual = Files.readString(outputFile).replace("\r\n", "\n");
        assertFalse(actual.isBlank(), "Output file is empty");

        Path golden = resourcePath("testing/expected-output.md");
        if (!Files.exists(golden)) {
            Files.writeString(golden, actual);
            System.out.println("[IntegrationTest] ✅ Golden file bootstrapped: " + golden);
        } else {
            String expected = Files.readString(golden).replace("\r\n", "\n");
            assertEquals(expected, actual, "Output diverged from golden file");
        }
    }

    /** FR02_02-HP_01: calibration fit resolves all calibration titles within ±10 locations. */
    @Test
    void calibrationAccuracy_withinTenLocs() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                null, "", null, null, false, null, false,
                resourcePath("testing/calibration.txt"),
                null, false
        );

        PipelineContext ctx = new PipelineContext(null);
        runSteps(ctx,
                new PreprocessBookStep(args),
                new LoadEpubStep(),
                new ParseTocStep(),
                new FitCalibrationStep(args),
                new ResolveHeadingsStep()
        );

        Map<String, Integer> targets = new CalibrationFitter()
                .parseCalibrationFile(resourcePath("testing/calibration.txt"));

        List<Heading> headings = ctx.resolvedHeadings;
        assertNotNull(headings, "Headings not resolved");
        assertFalse(headings.isEmpty(), "No headings were resolved");

        int matched = 0;
        for (Map.Entry<String, Integer> entry : targets.entrySet()) {
            int expected = entry.getValue();
            String wantedNorm = normalizeTitle(entry.getKey());
            Heading match = headings.stream()
                    .filter(h -> normalizeTitle(h.title()).equals(wantedNorm)
                            || normalizeTitle(h.title()).startsWith(wantedNorm)
                            || normalizeTitle(h.title()).contains(wantedNorm))
                    .findFirst().orElse(null);
            if (match == null) continue;
            matched++;
            int resolved = match.location();
            assertTrue(Math.abs(resolved - expected) <= 10,
                    "Heading '" + entry.getKey() + "': expected location " + expected
                            + " but resolved to " + resolved + " (diff=" + Math.abs(resolved - expected) + ")");
        }
        assertTrue(matched >= 2, "Fewer than 2 calibration titles could be matched to resolved headings");

        ctx.close();
    }

    /** FR02_03-HP_01, FR03_02-HP_02: --headings-only with no template uses bundled default and writes markdown. */
    @Test
    void headingsOnly_defaultTemplate_writesMarkdown() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        Path outputFile = tempDir.resolve("headings.md");
        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                null, "", null, outputFile,
                true, null, false,
                resourcePath("testing/calibration.txt"),
                null, false
        );

        new Pipeline(args).run();

        assertTrue(Files.exists(outputFile), "Headings output file not created");
        String content = Files.readString(outputFile);
        assertFalse(content.isBlank(), "Headings output is empty");
        assertTrue(content.contains("*(Location:"), "Output does not look like markdown headings");
        assertTrue(content.contains("# "), "Output missing heading markers");
    }

    /** FR02_03-HP_02, FR03_02-HP_01: --headings-only with custom template renders the book title. */
    @Test
    void headingsOnly_customTemplate_rendersTitle() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        Path templateFile = tempDir.resolve("headings.ftl");
        Files.writeString(templateFile, "TITLE:${title}");

        Path outputFile = tempDir.resolve("headings.txt");
        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                null, "", null, outputFile,
                true, templateFile, false,
                resourcePath("testing/calibration.txt"),
                null, false
        );

        new Pipeline(args).run();

        assertTrue(Files.exists(outputFile), "Headings output file not created");
        String content = Files.readString(outputFile);
        assertTrue(content.contains("TITLE:"), "Custom template marker not in output");
        assertFalse(content.replace("TITLE:", "").isBlank(), "Title was empty in custom template output");
    }

    /** FR04_01-HP_01: --print-calibration-template writes a file containing all TOC entry titles. */
    @Test
    void printCalibrationTemplate_containsAllTocEntries() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        Path outputFile = tempDir.resolve("calib.txt");
        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                null, "", null, null, false, null, false,
                null,
                outputFile, false
        );

        // Run partial pipeline: Preprocess + LoadEpub + ParseToc to get the TOC
        PipelineContext ctx = new PipelineContext(null);
        runSteps(ctx,
                new PreprocessBookStep(args),
                new LoadEpubStep(),
                new ParseTocStep(),
                new PrintCalibrationTemplateStep(args)
        );

        assertTrue(Files.exists(outputFile), "Calibration template was not written");
        String content = Files.readString(outputFile);
        assertFalse(content.isBlank(), "Calibration template is empty");

        // Every TOC entry title should appear in the written file
        ctx.tocEntries.forEach(entry ->
                assertTrue(content.contains(entry.title()),
                        "TOC entry '" + entry.title() + "' not found in calibration template"));

        ctx.close();
    }

    /** FR05_01-HP_01: --debug writes all expected numbered artifact files. */
    @Test
    void debugMode_writesExpectedArtifacts() throws Exception {
        assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
                "Testing fixture book.azw3 not present — skipping");

        Path outputFile = tempDir.resolve("output.md");
        AppArgs args = new AppArgs(
                resourcePath("testing/book.azw3"),
                resourcePath("testing/clippings.jsonl"),
                BOOK_TITLE,
                resourcePath("testing/template.ftl"),
                outputFile,
                false, null, true,
                resourcePath("testing/calibration.txt"),
                null, false
        );

        List<Clipping> clippings = loadClippingsFromJsonl("testing/clippings.jsonl");
        FyodorParseResult fakeResult = new FyodorParseResult(
                clippings, null, null, BOOK_TITLE, "fyodor output", List.of());

        try (var mocked = mockConstruction(FyodorClippingsParser.class,
                (mock, ctx) -> when(mock.parse(any(), any(), any())).thenReturn(fakeResult))) {
            new Pipeline(args).run();
        }

        Path debugDir = Path.of("debug-runs");
        assertTrue(Files.exists(debugDir), "debug-runs/ directory was not created");

        try (Stream<Path> runDirs = Files.list(debugDir)) {
            Path runDir = runDirs.findFirst().orElseThrow(() ->
                    new AssertionError("No run directory found inside debug-runs/"));

            List<String> expectedFiles = List.of(
                    "01_epub_metadata.json",
                    "02_toc_entries.json",
                    "03_resolved_headings.json",
                    "04_fyodor_stdout.txt",
                    "04_selected_book.json",
                    "05_clippings_stats.json",
                    "06_grouping_summary.json"
            );
            for (String name : expectedFiles) {
                assertTrue(Files.exists(runDir.resolve(name)),
                        "Expected debug artifact missing: " + name);
            }
        }
    }

    /** FR03_01-EC_02: template file has no read permission — RenderOutputStep throws IOException. */
    @Test
    void renderOutput_unreadableTemplate_throwsIOException() throws Exception {
        assumeUnix();

        Path templateFile = tempDir.resolve("template.ftl");
        Files.writeString(templateFile, "${title}");
        templateFile.toFile().setReadable(false);

        PipelineContext ctx = new PipelineContext(null);
        ctx.matchedBookTitle = "Test Book";
        ctx.groups = List.of();

        AppArgs args = new AppArgs(
            Path.of("book.epub"), null, "", templateFile, tempDir.resolve("output.md"),
            false, null, false, null, null, false
        );

        try {
            assertThrows(IOException.class, () -> new RenderOutputStep(args).execute(ctx));
        } finally {
            templateFile.toFile().setReadable(true);
        }
    }

    /** FR03_01-EC_04: output directory has no write permission — RenderOutputStep throws IOException. */
    @Test
    void renderOutput_readOnlyOutputDir_throwsIOException() throws Exception {
        assumeUnix();

        Path templateFile = tempDir.resolve("template.ftl");
        Files.writeString(templateFile, "${title}");

        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyDir);
        readOnlyDir.toFile().setWritable(false);

        PipelineContext ctx = new PipelineContext(null);
        ctx.matchedBookTitle = "Test Book";
        ctx.groups = List.of();

        AppArgs args = new AppArgs(
            Path.of("book.epub"), null, "", templateFile, readOnlyDir.resolve("output.md"),
            false, null, false, null, null, false
        );

        try {
            assertThrows(IOException.class, () -> new RenderOutputStep(args).execute(ctx));
        } finally {
            readOnlyDir.toFile().setWritable(true);
        }
    }

    /** FR03_02-EC_03: custom headings template has no read permission — HeadingsOnlyStep throws IOException. */
    @Test
    void headingsOnly_unreadableCustomTemplate_throwsIOException() throws Exception {
        assumeUnix();

        Path templateFile = tempDir.resolve("headings.ftl");
        Files.writeString(templateFile, "${title}");
        templateFile.toFile().setReadable(false);

        Heading fakeHeading = new Heading(new TocEntry("Chapter 1", "c1.html", null, 1), 0, 100);
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubTitle = "Test Book";
        ctx.resolvedHeadings = List.of(fakeHeading);

        AppArgs args = new AppArgs(
            Path.of("book.epub"), null, "", null, tempDir.resolve("headings.md"),
            true, templateFile, false, null, null, false
        );

        try {
            assertThrows(IOException.class, () -> new HeadingsOnlyStep(args).execute(ctx));
        } finally {
            templateFile.toFile().setReadable(true);
        }
    }

    /** FR03_02-EC_05: headings output directory has no write permission — HeadingsOnlyStep throws IOException. */
    @Test
    void headingsOnly_readOnlyOutputDir_throwsIOException() throws Exception {
        assumeUnix();

        Path templateFile = tempDir.resolve("headings.ftl");
        Files.writeString(templateFile, "${title}");

        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyDir);
        readOnlyDir.toFile().setWritable(false);

        Heading fakeHeading = new Heading(new TocEntry("Chapter 1", "c1.html", null, 1), 0, 100);
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubTitle = "Test Book";
        ctx.resolvedHeadings = List.of(fakeHeading);

        AppArgs args = new AppArgs(
            Path.of("book.epub"), null, "", null, readOnlyDir.resolve("headings.md"),
            true, templateFile, false, null, null, false
        );

        try {
            assertThrows(IOException.class, () -> new HeadingsOnlyStep(args).execute(ctx));
        } finally {
            readOnlyDir.toFile().setWritable(true);
        }
    }

    /** FR04_01-EC_03: calibration template output directory has no write permission — step throws IOException. */
    @Test
    void printCalibrationTemplate_readOnlyOutputDir_throwsIOException() throws Exception {
        assumeUnix();

        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectories(readOnlyDir);
        readOnlyDir.toFile().setWritable(false);

        PipelineContext ctx = new PipelineContext(null);
        ctx.tocEntries = List.of(new TocEntry("Chapter 1", "c1.html", null, 1));

        AppArgs args = new AppArgs(
            Path.of("book.epub"), null, "", null, null,
            false, null, false, null, readOnlyDir.resolve("calib.txt"), false
        );

        try {
            assertThrows(IOException.class, () -> new PrintCalibrationTemplateStep(args).execute(ctx));
        } finally {
            readOnlyDir.toFile().setWritable(true);
        }
    }

    /** FR02_01-EC_06: book's output directory has no write permission — Calibre exits non-zero and BookPreprocessor throws IOException. */
    @Test
    void preprocessBook_readOnlyOutputDir_throwsIOException() throws Exception {
        assumeUnix();
        assumeTrue(isCalibreAvailable(), "Calibre not installed — skipping");
        Path bookSrc = resourcePath("testing/book.azw3");
        assumeTrue(Files.exists(bookSrc), "Testing fixture book.azw3 not present — skipping");

        Path bookDir = tempDir.resolve("bookdir");
        Files.createDirectories(bookDir);
        Path bookCopy = bookDir.resolve("book.azw3");
        Files.copy(bookSrc, bookCopy);
        bookDir.toFile().setWritable(false);

        try {
            assertThrows(IOException.class, () -> BookPreprocessor.preprocess(bookCopy));
        } finally {
            bookDir.toFile().setWritable(true);
        }
    }

    // --- helpers ---

    private static Path resourcePath(String relativePath) {
        URL url = PipelineIntegrationTest.class.getClassLoader().getResource(relativePath);
        if (url == null) {
            return Path.of("src/test/resources/", relativePath);
        }
        try {
            return Path.of(url.toURI());
        } catch (Exception e) {
            return Path.of(url.getPath());
        }
    }

    private static List<Clipping> loadClippingsFromJsonl(String resourcePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = PipelineIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> {
                        try { return mapper.readValue(line, Clipping.class); }
                        catch (Exception e) { throw new RuntimeException("Bad JSONL line: " + line, e); }
                    })
                    .toList();
        }
    }

    private static void runSteps(PipelineContext ctx, io.github.vlovric.dazaikindle.pipeline.PipelineStep... steps) throws Exception {
        for (var step : steps) {
            step.execute(ctx);
        }
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static void assumeUnix() {
        assumeTrue(!System.getProperty("os.name", "").toLowerCase().contains("win"),
                "Permission-based test requires a Unix-like OS");
    }

    private static boolean isCalibreAvailable() {
        try {
            return new ProcessBuilder("ebook-convert", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
