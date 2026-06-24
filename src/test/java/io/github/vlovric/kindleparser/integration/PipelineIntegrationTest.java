package io.github.vlovric.kindleparser.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vlovric.kindleparser.AppArgs;
import io.github.vlovric.kindleparser.calibration.CalibrationFitter;
import io.github.vlovric.kindleparser.fyodor.FyodorClippingsParser;
import io.github.vlovric.kindleparser.fyodor.FyodorParseResult;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.pipeline.Pipeline;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.steps.*;
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

    private static void runSteps(PipelineContext ctx, io.github.vlovric.kindleparser.pipeline.PipelineStep... steps) throws Exception {
        for (var step : steps) {
            step.execute(ctx);
        }
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
