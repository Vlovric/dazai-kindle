package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.io.IOException;
import java.util.List;

import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparser.toc.TocParserResolver;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

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
