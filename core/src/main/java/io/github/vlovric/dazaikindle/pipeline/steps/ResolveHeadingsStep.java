package io.github.vlovric.dazaikindle.pipeline.steps;

import java.util.List;

import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.toc.LocationResolver;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.PipelineStep;
import io.github.vlovric.dazaikindle.pipeline.StepResult;

/**
 * Converts raw TOC entries into calibrated Kindle locations using the fit produced by
 * {@link FitCalibrationStep}. This is the last step that reads from {@link PipelineContext#epubLoader};
 * after it completes the loader is no longer needed (though it stays in the context until
 * {@link PipelineContext#close()} is called).
 */
public class ResolveHeadingsStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[DazaiKindle] 📍 Resolving locations for " + ctx.tocEntries.size() + " TOC entries...");

        LocationResolver resolver = new LocationResolver(ctx.epubLoader, ctx.bytesPerLocation, ctx.locationBias);
        List<Heading> headings = resolver.resolve(ctx.tocEntries);

        if (ctx.debug != null) {
            ctx.debug.writeJson("03_resolved_headings.json", headings);
            ctx.debug.writeJson("03_file_offsets.json", resolver.debugFileOffsets());
        }

        ctx.resolvedHeadings = headings;
        System.out.println("[DazaiKindle] 📍 Resolved " + headings.size() + " headings.");

        return StepResult.CONTINUE;
    }
}
