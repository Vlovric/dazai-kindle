package io.github.vlovric.dazaikindle.unit.pipeline.steps;

import io.github.vlovric.dazaikindle.EpubLoader;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.TocEntry;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.StepResult;
import io.github.vlovric.dazaikindle.pipeline.steps.ResolveHeadingsStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveHeadingsStepTest {

    @Mock
    EpubLoader mockLoader;

    /** Returns CONTINUE so the pipeline can proceed to HeadingsOnlyStep or ParseClippingsStep. */
    @Test
    void execute_alwaysReturnsContinue() throws Exception {
        when(mockLoader.getSpine()).thenReturn(List.of());
        PipelineContext ctx = buildCtx(List.of());

        StepResult result = new ResolveHeadingsStep().execute(ctx);

        assertEquals(StepResult.CONTINUE, result);
    }

    /** Populates ctx.resolvedHeadings even when TOC is empty. */
    @Test
    void execute_emptyToc_populatesEmptyHeadings() throws Exception {
        when(mockLoader.getSpine()).thenReturn(List.of());
        PipelineContext ctx = buildCtx(List.of());

        new ResolveHeadingsStep().execute(ctx);

        assertNotNull(ctx.resolvedHeadings);
        assertTrue(ctx.resolvedHeadings.isEmpty());
    }

    /** Resolves headings for entries whose file appears in the spine. */
    @Test
    void execute_spineEntryMatched_headingResolved() throws Exception {
        String spineFile = "OEBPS/chapter1.xhtml";
        String html = "<html><body><h1>Chapter One</h1></body></html>";

        EpubLoader.SpineItem spineItem = new EpubLoader.SpineItem("ch1", "chapter1.xhtml");
        when(mockLoader.getSpine()).thenReturn(List.of(spineItem));
        when(mockLoader.resolve("chapter1.xhtml")).thenReturn(spineFile);
        when(mockLoader.read(spineFile)).thenReturn(html.getBytes(StandardCharsets.UTF_8));

        TocEntry entry = new TocEntry("Chapter One", spineFile, null, 1);
        PipelineContext ctx = buildCtx(List.of(entry));

        new ResolveHeadingsStep().execute(ctx);

        assertNotNull(ctx.resolvedHeadings);
        assertEquals(1, ctx.resolvedHeadings.size());
        Heading h = ctx.resolvedHeadings.get(0);
        assertEquals("Chapter One", h.title());
        assertTrue(h.location() >= 1);
    }

    /** TOC entry whose file is not in the spine is silently dropped. */
    @Test
    void execute_tocEntryFileNotInSpine_dropped() throws Exception {
        when(mockLoader.getSpine()).thenReturn(List.of());
        TocEntry entry = new TocEntry("Orphan Chapter", "missing.xhtml", null, 1);
        PipelineContext ctx = buildCtx(List.of(entry));

        new ResolveHeadingsStep().execute(ctx);

        assertTrue(ctx.resolvedHeadings.isEmpty(), "Unresolvable TOC entries should be silently dropped");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PipelineContext buildCtx(List<TocEntry> entries) {
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubLoader = mockLoader;
        ctx.tocEntries = entries;
        ctx.bytesPerLocation = 128.0;
        ctx.locationBias = 0.0;
        return ctx;
    }
}
