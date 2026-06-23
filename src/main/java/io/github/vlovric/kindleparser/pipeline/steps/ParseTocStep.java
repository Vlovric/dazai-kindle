package io.github.vlovric.kindleparser.pipeline.steps;

import java.io.IOException;
import java.util.List;

import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparser.toc.TocParserResolver;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.PipelineStep;
import io.github.vlovric.kindleparser.pipeline.StepResult;

/**
 * Parses the EPUB table of contents into {@link PipelineContext#tocEntries}.
 * Supports both EPUB3 nav.xhtml and EPUB2 toc.ncx formats via {@link io.github.vlovric.kindleparser.toc.TocParserResolver}.
 * Throws if the TOC is empty — an empty TOC means there is no structure to group clippings under.
 */
public class ParseTocStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[KindleParser] 📑 Parsing table of contents...");

        List<TocEntry> tocEntries = new TocParserResolver(ctx.epubLoader).parse();

        if (ctx.debug != null) {
            ctx.debug.writeJson("02_toc_entries.json", tocEntries);
            ctx.debug.writeText("02_toc_source.txt",
                "tocHref=" + ctx.epubLoader.getTocHref() +
                "\nresolved=" + ctx.epubLoader.resolve(ctx.epubLoader.getTocHref()) + "\n"
            );
        }

        if (tocEntries.isEmpty()) {
            throw new IOException("[KindleParser] ❌ No TOC entries found. Does the EPUB contain a toc.ncx or nav.xhtml?");
        }

        ctx.tocEntries = tocEntries;
        System.out.println("[KindleParser] 📑 Found " + tocEntries.size() + " TOC entries.");

        return StepResult.CONTINUE;
    }
}
