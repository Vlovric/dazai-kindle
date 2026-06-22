package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.util.List;
import java.util.Map;

import io.github.vlovric.kindleparser.Grouper;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

public class GroupClippingsStep implements PipelineStep {

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        System.out.println("[KindleParser] 🗂️  Grouping under headings...");

        List<HeadingGroup> groups = new Grouper().group(ctx.clippings, ctx.resolvedHeadings);

        if (ctx.debug != null) {
            ctx.debug.writeJson("06_grouping_summary.json", groups.stream().map(g -> Map.<String, Object>of(
                    "headingTitle", g.heading().title(),
                    "headingLocation", g.heading().location(),
                    "level", g.heading().level(),
                    "clippingCount", g.clippings().size()
            )).toList());

            HeadingGroup beforeFirst = groups.stream()
                    .filter(g -> g.heading().title().equals("(Before first heading)"))
                    .findFirst().orElse(null);
            if (beforeFirst != null) {
                ctx.debug.writeJson("06_before_first_bucket.json", beforeFirst.clippings());
            }
        }

        System.out.println("[KindleParser]    Grouped into " + groups.size() + " sections.");

        ctx.groups = groups;
        return StepResult.CONTINUE;
    }
}
