package io.github.vlovric.dazaikindle.pipeline.steps;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.vlovric.dazaikindle.fyodor.FyodorClippingsParser;
import io.github.vlovric.dazaikindle.fyodor.FyodorParseResult;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.PipelineStep;
import io.github.vlovric.dazaikindle.pipeline.StepResult;

/**
 * Pipeline step that parses Kindle clippings for the target book via Fyodor.
 * Populates {@link PipelineContext#clippings} and {@link PipelineContext#matchedBookTitle}.
 * When debug mode is active, writes Fyodor stdout, the output file listing, the selected
 * book summary, and clipping statistics as numbered debug artifacts (04_* and 05_*).
 */
public class ParseClippingsStep implements PipelineStep {

    private final AppArgs args;

    /**
     * @param args resolved CLI arguments; provides the clippings file path, title filter,
     *             and the overwrite-fyodor-template flag
     */
    public ParseClippingsStep(AppArgs args) {
        this.args = args;
    }

    /**
     * Validates that at least one book identifier is available, runs {@link FyodorClippingsParser},
     * writes debug artifacts, and populates the context with the parsed clippings.
     *
     * @param ctx mutable pipeline state; reads {@code epubTitle} and {@code debug},
     *            writes {@code clippings} and {@code matchedBookTitle}
     * @return {@link StepResult#CONTINUE} on success
     * @throws IOException if the book cannot be identified, Fyodor fails, no clippings are found,
     *                     or debug artifacts cannot be written
     * @throws InterruptedException if the Fyodor subprocess is interrupted
     */
    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        String titleFilter = args.titleFilter();

        // FR01_01-EC_10: no EPUB title and no title filter → can't identify the book
        if ((ctx.epubTitle == null || ctx.epubTitle.isBlank()) &&
                (titleFilter == null || titleFilter.isBlank())) {
            throw new IOException(
                    "[DazaiKindle] ❌ Cannot identify book: EPUB has no title and no --title filter was provided.");
        }

        System.out.println("[DazaiKindle] ✂️  Parsing clippings via Fyodor: " + args.clippings());

        Path fyodorOutDir = ctx.debug != null ? ctx.debug.runDir().resolve("fyodor-out") : null;
        FyodorClippingsParser parser = new FyodorClippingsParser(args.clippings(), args.overwriteFyodorTemplate());
        FyodorParseResult result = parser.parse(titleFilter, ctx.epubTitle, fyodorOutDir);

        writeDebugArtifacts(ctx, result, titleFilter);

        List<Clipping> clippings = result.clippings();

        // FR01_01-EC_05 / EC_14: fyodor produced no usable output for this book
        if (clippings.isEmpty()) {
            String suffix = (titleFilter != null && !titleFilter.isBlank())
                    ? " matching '" + titleFilter + "'"
                    : "";
            throw new IOException("[DazaiKindle] ❌ No clippings found" + suffix
                    + ". Check the clippings file or provide a more specific --title filter.");
        }

        String matchedBookTitle = result.selectedBookTitle();
        if (matchedBookTitle == null || matchedBookTitle.isBlank()) {
            matchedBookTitle = clippings.get(0).bookTitle();
        }

        System.out.println("[DazaiKindle]    Found " + clippings.size() + " clipping(s) for '" + matchedBookTitle + "'");

        writeClippingsStatsDebug(ctx, result, clippings);

        ctx.clippings = clippings;
        ctx.matchedBookTitle = matchedBookTitle;

        return StepResult.CONTINUE;
    }

    /**
     * Writes Fyodor process artifacts to the debug run directory:
     * <ul>
     *   <li>{@code 04_fyodor_stdout.txt} — captured stdout/stderr from the Fyodor process</li>
     *   <li>{@code 04_fyodor_out_listing.json} — all files Fyodor wrote, with sizes</li>
     *   <li>{@code 04_selected_book.json} — which file was selected and the title signals used</li>
     * </ul>
     * No-op when {@code ctx.debug} is {@code null}.
     *
     * @param ctx         pipeline context supplying the debug writer and EPUB title
     * @param result      parse result from {@link FyodorClippingsParser}
     * @param titleFilter the {@code --title} filter value, or {@code null}
     * @throws IOException if any artifact cannot be written
     */
    private void writeDebugArtifacts(PipelineContext ctx, FyodorParseResult result, String titleFilter) throws IOException {
        if (ctx.debug == null) return;

        ctx.debug.writeText("04_fyodor_stdout.txt", result.fyodorStdout() != null ? result.fyodorStdout() : "");

        List<Path> outputFiles = result.outputFiles() != null ? result.outputFiles() : List.of();
        ctx.debug.writeJson("04_fyodor_out_listing.json", outputFiles.stream().map(p -> {
            try {
                return Map.of(
                        "file", p.getFileName().toString(),
                        "size", java.nio.file.Files.size(p),
                        "path", p.toAbsolutePath().toString()
                );
            } catch (IOException e) {
                return Map.of(
                        "file", p.getFileName().toString(),
                        "path", p.toAbsolutePath().toString(),
                        "error", e.getMessage()
                );
            }
        }).toList());

        ctx.debug.writeJson("04_selected_book.json", Map.of(
                "selectedFile", result.selectedFile() == null ? "" : result.selectedFile().toAbsolutePath().toString(),
                "selectedBookTitle", result.selectedBookTitle() != null ? result.selectedBookTitle() : "",
                "expectedEpubTitle", ctx.epubTitle != null ? ctx.epubTitle : "",
                "titleFilter", titleFilter != null ? titleFilter : ""
        ));
    }

    /**
     * Writes clipping statistics to the debug run directory after a successful parse:
     * <ul>
     *   <li>{@code 05_clippings_stats.json} — count, type breakdown, location range, null-location count</li>
     *   <li>{@code 05_clippings_sample.json} — first 25 clippings for spot-checking</li>
     * </ul>
     * No-op when {@code ctx.debug} is {@code null}.
     *
     * @param ctx      pipeline context supplying the debug writer and EPUB title
     * @param result   parse result, used for selected file and book title metadata
     * @param clippings the full list of parsed clippings for the target book
     * @throws IOException if either artifact cannot be written
     */
    private void writeClippingsStatsDebug(PipelineContext ctx, FyodorParseResult result, List<Clipping> clippings) throws IOException {
        if (ctx.debug == null) return;

        Map<String, Integer> typeCounts = new HashMap<>();
        int nullLoc = 0;
        Integer minLoc = null;
        Integer maxLoc = null;

        for (Clipping c : clippings) {
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

        ctx.debug.writeJson("05_clippings_stats.json", Map.of(
                "selectedFile", result.selectedFile() == null ? "" : result.selectedFile().toAbsolutePath().toString(),
                "selectedBookTitle", result.selectedBookTitle() != null ? result.selectedBookTitle() : "",
                "expectedEpubTitle", ctx.epubTitle != null ? ctx.epubTitle : "",
                "count", clippings.size(),
                "typeCounts", typeCounts,
                "nullLocationCount", nullLoc,
                "minLocation", minLoc != null ? minLoc : 0,
                "maxLocation", maxLoc != null ? maxLoc : 0
        ));
        ctx.debug.writeJson("05_clippings_sample.json", clippings.stream().limit(25).toList());
    }
}
