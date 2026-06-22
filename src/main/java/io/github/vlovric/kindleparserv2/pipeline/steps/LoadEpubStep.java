package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.util.Map;

import io.github.vlovric.kindleparser.EpubLoader;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

public class LoadEpubStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[KindleParser] 📖 Loading EPUB: " + ctx.bookPath);

        EpubLoader loader = ctx.debug != null
            ? new EpubLoader(ctx.bookPath, ctx.debug.runDir().resolve("01_epub_extracted"), true)
            : new EpubLoader(ctx.bookPath);

        loader.open();

        ctx.epubLoader = loader;
        ctx.epubTitle  = loader.getBookTitle();
        ctx.epubAuthor = loader.getBookAuthor();

        if (ctx.debug != null) {
            ctx.debug.writeJson("01_epub_metadata.json", Map.of(
                "epubPath",   ctx.bookPath.toAbsolutePath().toString(),
                "extractDir", loader.getExtractDir().toAbsolutePath().toString(),
                "opfRoot",    loader.getOpfRoot(),
                "tocHref",    loader.getTocHref(),
                "title",      ctx.epubTitle  != null ? ctx.epubTitle  : "",
                "author",     ctx.epubAuthor != null ? ctx.epubAuthor : ""
            ));
            ctx.debug.writeJson("01_spine.json", loader.getSpine());
        }

        return StepResult.CONTINUE;
    }
}
