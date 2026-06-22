package io.github.vlovric.kindleparser.calibration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.vlovric.kindleparser.EpubLoader;
import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparser.toc.LocationResolver;

/**
 * Parses a user-provided calibration file and fits a linear model (bytes → Kindle location).
 *
 * Calibration file format (one entry per line):
 *   Heading Title - 1234
 *   Heading Title: 1234
 *   Heading Title — 1234   (em dash)
 *   Heading Title – 1234   (en dash)
 * Lines starting with '#' and blank lines are ignored.
 */
public class CalibrationFitter {

    // Supports: "-", ":", "—" (U+2014 em dash), "–" (U+2013 en dash)
    private static final Pattern LINE_PATTERN =
        Pattern.compile("^\\s*(.+?)\\s*(?:-|:|—|–)\\s*(\\d+)\\s*$");

    /**
     * Parses the calibration file into a title → Kindle location map.
     *
     * @throws IOException if the file cannot be read or contains a location <= 0 (FR02_02-EC_08)
     * @throws IOException if the file contains no usable entries (FR02_02-EC_06)
     */
    public Map<String, Integer> parseCalibrationFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Map<String, Integer> out = new LinkedHashMap<>();

        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            Matcher m = LINE_PATTERN.matcher(trimmed);
            if (!m.matches()) continue;

            String title = m.group(1).trim();
            int loc = Integer.parseInt(m.group(2));

            if (loc <= 0) {
                throw new IOException(
                    "[KindleParser] ❌ Calibration file contains a non-positive location (" + loc + ") for: '" + title + "'"
                );
            }
            if (!title.isEmpty()) {
                out.put(title, loc);
            }
        }

        if (out.isEmpty()) {
            throw new IOException(
                "[KindleParser] ❌ Calibration file has no usable entries. " +
                "Use format: 'Heading Title - 123'. File: " + file
            );
        }

        return out;
    }

    /**
     * Matches calibration targets to TOC entries, resolves their byte offsets, and fits a linear model.
     *
     * @throws IOException for < 2 matched points, zero variance, or non-positive slope (FR02_02-EC_10/11/12)
     */
    public CalibrationFit fit(
        Map<String, Integer> targets,
        EpubLoader loader,
        List<TocEntry> tocEntries
    ) throws IOException {
        LocationResolver uncalibrated = new LocationResolver(loader);

        List<double[]> points = new ArrayList<>();
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
                System.out.println("[KindleParser] ⚠️  Calibration anchor not resolvable for: '" + match.title() + "'");
                notResolvable++;
                continue;
            }

            System.out.println("[KindleParser]    '" + wantedTitle + "' -> '" + match.title()
                + "' @ byteOffset=" + byteOffset + " => location=" + wantedLoc);
            points.add(new double[]{byteOffset, wantedLoc - 1});
        }

        System.out.println("[KindleParser] 📌 Calibration points: used=" + points.size()
            + ", notFoundInToc=" + notFound + ", notResolvable=" + notResolvable);

        if (points.size() < 2) {
            throw new IOException(
                "[KindleParser] ❌ Need at least 2 matched calibration points; got " + points.size()
            );
        }

        return fitLeastSquares(points);
    }

    /**
     * Least-squares linear fit on (byteOffset, kindleLocation-1) pairs.
     * Exposed package-private for unit testing.
     */
    CalibrationFit fitLeastSquares(List<double[]> points) throws IOException {
        int n = points.size();

        double mx = points.stream().mapToDouble(p -> p[0]).average().orElse(0);
        double my = points.stream().mapToDouble(p -> p[1]).average().orElse(0);

        double num = 0, den = 0;
        for (double[] p : points) {
            num += (p[0] - mx) * (p[1] - my);
            den += (p[0] - mx) * (p[0] - mx);
        }

        if (den == 0) {
            throw new IOException(
                "[KindleParser] ❌ Calibration points have zero variance in byte offsets (cannot fit)"
            );
        }

        double slope = num / den;
        if (slope <= 0) {
            throw new IOException(
                "[KindleParser] ❌ Calibration fit produced non-positive slope (slope=" + slope + ")"
            );
        }

        double intercept = my - (slope * mx);
        double bytesPerLocation = 1.0 / slope;
        double locationBias = intercept;

        double se = 0;
        for (double[] p : points) {
            double err = p[1] - (slope * p[0] + intercept);
            se += err * err;
        }
        double rmse = Math.sqrt(se / n);

        return new CalibrationFit(bytesPerLocation, locationBias, n, rmse);
    }

    /**
     * Fuzzy-matches a calibration title to a TOC entry.
     * Priority: exact match > prefix match > contains match.
     */
    TocEntry findTocEntry(List<TocEntry> tocEntries, String wantedTitle) {
        String wantedNorm = normalizeTitle(wantedTitle);
        if (wantedNorm.isEmpty()) return null;

        TocEntry exact = null, prefix = null, contains = null;

        for (TocEntry e : tocEntries) {
            String candNorm = normalizeTitle(e.title());
            if (candNorm.equals(wantedNorm)) { exact = e; break; }
            if (prefix   == null && candNorm.startsWith(wantedNorm)) prefix = e;
            if (contains == null && candNorm.contains(wantedNorm))   contains = e;
        }

        if (exact    != null) return exact;
        if (prefix   != null) return prefix;
        return contains;
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
