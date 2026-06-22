package io.github.vlovric.kindleparser.calibration;

import io.github.vlovric.kindleparser.calibration.CalibrationFit;
import io.github.vlovric.kindleparser.calibration.CalibrationFitter;
import io.github.vlovric.kindleparser.models.TocEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalibrationFitterTest {

    @TempDir
    Path tempDir;

    private final CalibrationFitter fitter = new CalibrationFitter();

    // ── parseCalibrationFile ───────────────────────────────────────────────────

    /** FR02_02-EC_04 + EC_05: standard delimiter "-" with surrounding whitespace. */
    @Test
    void parse_dashDelimiterWithWhitespace() throws Exception {
        Path f = write("Chapter One - 100\n");
        Map<String, Integer> result = fitter.parseCalibrationFile(f);
        assertEquals(100, result.get("Chapter One"));
    }

    /** FR02_02-EC_04: colon delimiter. */
    @Test
    void parse_colonDelimiter() throws Exception {
        Path f = write("Chapter One: 200\n");
        assertEquals(200, fitter.parseCalibrationFile(f).get("Chapter One"));
    }

    /** FR02_02-EC_04: em dash delimiter (—). */
    @Test
    void parse_emDashDelimiter() throws Exception {
        Path f = write("Chapter One — 300\n");
        assertEquals(300, fitter.parseCalibrationFile(f).get("Chapter One"));
    }

    /** FR02_02-EC_04 (variant): en dash delimiter (–). */
    @Test
    void parse_enDashDelimiter() throws Exception {
        Path f = write("Chapter One – 400\n");
        assertEquals(400, fitter.parseCalibrationFile(f).get("Chapter One"));
    }

    /** FR02_02-EC_03: unsupported delimiter → line skipped. */
    @Test
    void parse_unsupportedDelimiterSkipped() throws Exception {
        // Also include one valid line so the file isn't empty
        Path f = write("Chapter One | 100\nValid Chapter - 200\n");
        Map<String, Integer> result = fitter.parseCalibrationFile(f);
        assertFalse(result.containsKey("Chapter One"));
        assertTrue(result.containsKey("Valid Chapter"));
    }

    /** Comment lines and blank lines are silently ignored. */
    @Test
    void parse_commentsAndBlanksIgnored() throws Exception {
        Path f = write("# This is a comment\n\nChapter One - 100\n");
        Map<String, Integer> result = fitter.parseCalibrationFile(f);
        assertEquals(1, result.size());
        assertEquals(100, result.get("Chapter One"));
    }

    /** Blank location (e.g. "Chapter One - ") is skipped — user hasn't filled it in yet. */
    @Test
    void parse_blankLocationSkipped() throws Exception {
        // "Chapter One - " — the regex requires \d+ so this won't match → skipped
        // Include one valid line to avoid empty-file error
        Path f = write("Chapter One - \nChapter Two - 200\n");
        Map<String, Integer> result = fitter.parseCalibrationFile(f);
        assertFalse(result.containsKey("Chapter One"));
        assertEquals(200, result.get("Chapter Two"));
    }

    /** FR02_02-EC_07: calibration file does not exist → IOException. */
    @Test
    void parse_nonExistentFileThrows() {
        Path missing = tempDir.resolve("missing.txt");
        assertThrows(IOException.class, () -> fitter.parseCalibrationFile(missing));
    }

    /** FR02_02-EC_06: empty calibration file → IOException. */
    @Test
    void parse_emptyFileThrows() throws Exception {
        Path f = write("");
        assertThrows(IOException.class, () -> fitter.parseCalibrationFile(f));
    }

    /** FR02_02-EC_06: file with only comments → IOException (no usable entries). */
    @Test
    void parse_onlyCommentsThrows() throws Exception {
        Path f = write("# just a comment\n# another\n");
        assertThrows(IOException.class, () -> fitter.parseCalibrationFile(f));
    }

    /** FR02_02-EC_08: location is 0 → IOException. */
    @Test
    void parse_zeroLocationThrows() throws Exception {
        Path f = write("Chapter One - 0\n");
        IOException ex = assertThrows(IOException.class, () -> fitter.parseCalibrationFile(f));
        assertTrue(ex.getMessage().contains("0"));
    }

    // ── fitLeastSquares ────────────────────────────────────────────────────────

    /** FR02_02-HP_01: two perfect collinear points → exact fit, rmse=0. */
    @Test
    void fit_perfectTwoPoints() throws Exception {
        // y = 0.01 * x  =>  bytesPerLoc = 100, bias = 0
        List<double[]> pts = List.of(new double[]{1000, 10}, new double[]{2000, 20});
        CalibrationFit fit = fitter.fitLeastSquares(pts);
        assertEquals(100.0, fit.bytesPerLocation(), 0.001);
        assertEquals(0.0,   fit.locationBias(),     0.001);
        assertEquals(0.0,   fit.rmseLocations(),    0.001);
        assertEquals(2,     fit.pointsUsed());
    }

    /** FR02_02-HP_01: more than 2 points, slight noise → reasonable fit. */
    @Test
    void fit_multiplePointsReasonableFit() throws Exception {
        List<double[]> pts = List.of(
            new double[]{1000, 10},
            new double[]{2000, 20},
            new double[]{3000, 30},
            new double[]{4000, 40}
        );
        CalibrationFit fit = fitter.fitLeastSquares(pts);
        assertEquals(100.0, fit.bytesPerLocation(), 1.0);
        assertTrue(fit.rmseLocations() < 1.0);
        assertEquals(4, fit.pointsUsed());
    }

    /** FR02_02-EC_10: only one point → IOException via fit() method. */
    @Test
    void fit_singlePointThrowsViaFitMethod() throws Exception {
        Path calibFile = write("Chapter One - 100\n");
        // fit() checks points.size() < 2 only after fuzzy matching, but we can test directly
        // via fitLeastSquares with a list of 1:
        List<double[]> pts = List.of(new double[]{1000, 100});
        // fitLeastSquares itself doesn't enforce >=2 (that check is in fit()), it will just fail on variance
        // With one point variance is 0
        assertThrows(IOException.class, () -> fitter.fitLeastSquares(pts));
    }

    /** FR02_02-EC_11: all points at same byte offset → zero variance → IOException. */
    @Test
    void fit_zeroVarianceThrows() {
        List<double[]> pts = List.of(
            new double[]{1000, 10},
            new double[]{1000, 20}
        );
        assertThrows(IOException.class, () -> fitter.fitLeastSquares(pts));
    }

    /** FR02_02-EC_12: negative slope (locations decrease as bytes increase) → IOException. */
    @Test
    void fit_negativeSlopeThrows() {
        List<double[]> pts = List.of(
            new double[]{1000, 100},
            new double[]{2000, 50}
        );
        assertThrows(IOException.class, () -> fitter.fitLeastSquares(pts));
    }

    // ── findTocEntry ───────────────────────────────────────────────────────────

    /** FR02_02-EC_02: exact match preferred over prefix/contains. */
    @Test
    void findTocEntry_exactMatchWins() {
        List<TocEntry> toc = List.of(
            entry("Introduction to Everything"),
            entry("Introduction"),
            entry("My Introduction Section")
        );
        TocEntry result = fitter.findTocEntry(toc, "Introduction");
        assertEquals("Introduction", result.title());
    }

    /** FR02_02-EC_02: prefix match when no exact. */
    @Test
    void findTocEntry_prefixMatchFallback() {
        List<TocEntry> toc = List.of(entry("Introduction to Java"), entry("Conclusion"));
        TocEntry result = fitter.findTocEntry(toc, "Introduction");
        assertEquals("Introduction to Java", result.title());
    }

    /** FR02_02-EC_02: contains match as last resort. */
    @Test
    void findTocEntry_containsMatchFallback() {
        List<TocEntry> toc = List.of(entry("Part One: Introduction"), entry("Conclusion"));
        TocEntry result = fitter.findTocEntry(toc, "Introduction");
        assertEquals("Part One: Introduction", result.title());
    }

    /** FR02_02-EC_09: no match at all → null, caller should log and skip. */
    @Test
    void findTocEntry_noMatchReturnsNull() {
        List<TocEntry> toc = List.of(entry("Chapter One"), entry("Chapter Two"));
        assertNull(fitter.findTocEntry(toc, "Nonexistent Heading"));
    }

    /** Matching is case-insensitive and punctuation-tolerant. */
    @Test
    void findTocEntry_caseAndPunctuationInsensitive() {
        List<TocEntry> toc = List.of(entry("Chapter One: The Beginning"));
        TocEntry result = fitter.findTocEntry(toc, "chapter one the beginning");
        assertNotNull(result);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Path write(String content) throws Exception {
        Path f = tempDir.resolve("calibration.txt");
        Files.writeString(f, content, StandardCharsets.UTF_8);
        return f;
    }

    private static TocEntry entry(String title) {
        return new TocEntry(title, "file.xhtml", null, 1);
    }
}
