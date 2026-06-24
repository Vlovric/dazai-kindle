package io.github.vlovric.dazaikindle.pipeline.steps;

import java.io.IOException;
import java.util.List;

import io.github.vlovric.dazaikindle.models.TocEntry;
import io.github.vlovric.dazaikindle.toc.TocParserResolver;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.PipelineStep;
import io.github.vlovric.dazaikindle.pipeline.StepResult;

/**
 * Parses the EPUB table of contents into {@link PipelineContext#tocEntries}.
 * Supports both EPUB3 nav.xhtml and EPUB2 toc.ncx formats via {@link io.github.vlovric.dazaikindle.toc.TocParserResolver}.
 * Throws if the TOC is empty — an empty TOC means there is no structure to group clippings under.
 */
public class ParseTocStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[DazaiKindle] 📑 Parsing table of contents...");

        List<TocEntry> tocEntries = new TocParserResolver(ctx.epubLoader).parse();

        if (ctx.debug != null) {
            ctx.debug.writeJson("02_toc_entries.json", tocEntries);
            ctx.debug.writeText("02_toc_source.txt",
                "tocHref=" + ctx.epubLoader.getTocHref() +
                "\nresolved=" + ctx.epubLoader.resolve(ctx.epubLoader.getTocHref()) + "\n"
            );
        }

        if (tocEntries.isEmpty()) {
            throw new IOException("[DazaiKindle] ❌ No TOC entries found. Does the EPUB contain a toc.ncx or nav.xhtml?");
        }

        ctx.tocEntries = tocEntries;
        System.out.println("[DazaiKindle] 📑 Found " + tocEntries.size() + " TOC entries.");

        return StepResult.CONTINUE;
    }
}
