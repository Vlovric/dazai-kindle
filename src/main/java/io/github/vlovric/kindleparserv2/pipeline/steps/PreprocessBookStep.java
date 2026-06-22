package io.github.vlovric.kindleparserv2.pipeline.steps;

import io.github.vlovric.kindleparser.calibre.BookPreprocessor;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

/**
 * PipelineStep that preprocesses the input book file.
 * If the file is in .azw3 or .mobi format, it is converted to .epub using Calibre's ebook-convert tool.
 */
public class PreprocessBookStep implements PipelineStep {

    private final AppArgs args;

    public PreprocessBookStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[KindleParser] Checking input file...");
        ctx.bookPath = BookPreprocessor.preprocess(args.book());
        System.out.println("[KindleParser] 📖 Loaded: " + ctx.bookPath);
        return StepResult.CONTINUE;
    }
}
