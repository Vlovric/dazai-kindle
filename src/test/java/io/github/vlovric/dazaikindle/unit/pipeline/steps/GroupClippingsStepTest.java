package io.github.vlovric.dazaikindle.unit.pipeline.steps;

import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.TocEntry;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.StepResult;
import io.github.vlovric.dazaikindle.pipeline.steps.GroupClippingsStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GroupClippingsStepTest {

    /** HP_01: clippings and headings present → groups populated, returns CONTINUE. */
    @Test
    void execute_withClippingsAndHeadings_populatesGroups() throws Exception {
        Heading heading = new Heading(new TocEntry("Chapter 1", "ch1.xhtml", null, 1), 0, 100);
        Clipping clipping = new Clipping("Book", "Author", "highlight", "150", null, "2024", "text");

        PipelineContext ctx = new PipelineContext(null);
        ctx.clippings = List.of(clipping);
        ctx.resolvedHeadings = List.of(heading);

        StepResult result = new GroupClippingsStep().execute(ctx);

        assertEquals(StepResult.CONTINUE, result);
        assertNotNull(ctx.groups);
        assertFalse(ctx.groups.isEmpty());
    }

    /** HP_01: clipping before first heading goes into the "(Before first heading)" sentinel group. */
    @Test
    void execute_clippingBeforeFirstHeading_goesIntoSentinelGroup() throws Exception {
        Heading heading = new Heading(new TocEntry("Chapter 1", "ch1.xhtml", null, 1), 0, 500);
        Clipping clipping = new Clipping("Book", "Author", "highlight", "10", null, "2024", "early text");

        PipelineContext ctx = new PipelineContext(null);
        ctx.clippings = List.of(clipping);
        ctx.resolvedHeadings = List.of(heading);

        new GroupClippingsStep().execute(ctx);

        boolean hasSentinel = ctx.groups.stream()
                .anyMatch(g -> g.heading().title().equals("(Before first heading)") && !g.clippings().isEmpty());
        assertTrue(hasSentinel, "Clipping before first heading should be in sentinel group");
    }

    /** EC: no clippings → groups contain only the sentinel with zero entries. */
    @Test
    void execute_noClippings_onlySentinelGroup() throws Exception {
        Heading heading = new Heading(new TocEntry("Chapter 1", "ch1.xhtml", null, 1), 0, 100);

        PipelineContext ctx = new PipelineContext(null);
        ctx.clippings = List.of();
        ctx.resolvedHeadings = List.of(heading);

        new GroupClippingsStep().execute(ctx);

        int totalClippings = ctx.groups.stream().mapToInt(g -> g.clippings().size()).sum();
        assertEquals(0, totalClippings);
    }
}
