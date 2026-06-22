package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.util.List;

import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.toc.LocationResolver;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

public class ResolveHeadingsStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[KindleParser] 📍 Resolving locations for " + ctx.tocEntries.size() + " TOC entries...");

        LocationResolver resolver = new LocationResolver(ctx.epubLoader, ctx.bytesPerLocation, ctx.locationBias);
        List<Heading> headings = resolver.resolve(ctx.tocEntries);

        if (ctx.debug != null) {
            ctx.debug.writeJson("03_resolved_headings.json", headings);
            ctx.debug.writeJson("03_file_offsets.json", resolver.debugFileOffsets());
        }

        ctx.resolvedHeadings = headings;
        System.out.println("[KindleParser] 📍 Resolved " + headings.size() + " headings.");

        return StepResult.CONTINUE;
    }
}
