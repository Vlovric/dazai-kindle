package io.github.vlovric.kindleparser.pipeline.steps;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.vlovric.kindleparser.fyodor.FyodorClippingsParser;
import io.github.vlovric.kindleparser.fyodor.FyodorParseResult;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.AppArgs;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.PipelineStep;
import io.github.vlovric.kindleparser.pipeline.StepResult;

public class ParseClippingsStep implements PipelineStep {

    private final AppArgs args;

    public ParseClippingsStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        String titleFilter = args.titleFilter();

        // FR01_01-EC_10: no EPUB title and no title filter → can't identify the book
        if ((ctx.epubTitle == null || ctx.epubTitle.isBlank()) &&
                (titleFilter == null || titleFilter.isBlank())) {
            throw new IOException(
                    "[KindleParser] ❌ Cannot identify book: EPUB has no title and no --title filter was provided.");
        }

        System.out.println("[KindleParser] ✂️  Parsing clippings via Fyodor: " + args.clippings());

        Path fyodorOutDir = ctx.debug != null ? ctx.debug.runDir().resolve("fyodor-out") : null;
        FyodorClippingsParser parser = new FyodorClippingsParser(args.clippings(), args.overwriteFyodorTemplate());
        FyodorParseResult result = parser.parse(titleFilter, ctx.epubTitle, fyodorOutDir, ctx.debug);

        List<Clipping> clippings = result.clippings();

        // FR01_01-EC_05 / EC_14: fyodor produced no usable output for this book
        if (clippings.isEmpty()) {
            String suffix = (titleFilter != null && !titleFilter.isBlank())
                    ? " matching '" + titleFilter + "'"
                    : "";
            throw new IOException("[KindleParser] ❌ No clippings found" + suffix
                    + ". Check the clippings file or provide a more specific --title filter.");
        }

        String matchedBookTitle = result.selectedBookTitle();
        if (matchedBookTitle == null || matchedBookTitle.isBlank()) {
            matchedBookTitle = clippings.get(0).bookTitle();
        }

        System.out.println("[KindleParser]    Found " + clippings.size() + " clipping(s) for '" + matchedBookTitle + "'");

        if (ctx.debug != null) {
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
                    "selectedFile", result.selectedFile() == null ? null : result.selectedFile().toAbsolutePath().toString(),
                    "selectedBookTitle", result.selectedBookTitle(),
                    "expectedEpubTitle", ctx.epubTitle,
                    "count", clippings.size(),
                    "typeCounts", typeCounts,
                    "nullLocationCount", nullLoc,
                    "minLocation", minLoc,
                    "maxLocation", maxLoc
            ));
            ctx.debug.writeJson("05_clippings_sample.json", clippings.stream().limit(25).toList());
        }

        ctx.clippings = clippings;
        ctx.matchedBookTitle = matchedBookTitle;

        return StepResult.CONTINUE;
    }
}
