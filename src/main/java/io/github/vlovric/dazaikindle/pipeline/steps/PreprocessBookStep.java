package io.github.vlovric.dazaikindle.pipeline.steps;

import io.github.vlovric.dazaikindle.calibre.BookPreprocessor;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.PipelineStep;
import io.github.vlovric.dazaikindle.pipeline.StepResult;

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
        System.out.println("[DazaiKindle] Checking input file...");
        ctx.bookPath = BookPreprocessor.preprocess(args.book());
        System.out.println("[DazaiKindle] 📖 Loaded: " + ctx.bookPath);
        return StepResult.CONTINUE;
    }
}
