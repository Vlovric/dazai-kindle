package io.github.vlovric.kindleparser.unit.pipeline.steps;

import io.github.vlovric.kindleparser.EpubLoader;
import io.github.vlovric.kindleparser.toc.TocParserResolver;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.StepResult;
import io.github.vlovric.kindleparser.pipeline.steps.ParseTocStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParseTocStepTest {

    @Mock
    EpubLoader mockLoader;

    /** FR02_02-EC_01: empty TOC → IOException, pipeline stops. */
    @Test
    void execute_emptyToc_throwsIOException() throws Exception {
        try (var mocked = mockConstruction(TocParserResolver.class, (mock, ctx) ->
                when(mock.parse()).thenReturn(List.of()))) {

            PipelineContext ctx = buildCtx();
            assertThrows(IOException.class, () -> new ParseTocStep().execute(ctx));
        }
    }

    /** FR02_02-HP_01: non-empty TOC → populated in context, returns CONTINUE. */
    @Test
    void execute_validToc_populatesContextAndContinues() throws Exception {
        var entry = new io.github.vlovric.kindleparser.models.TocEntry("Chapter One", "ch1.xhtml", null, 1);

        try (var mocked = mockConstruction(TocParserResolver.class, (mock, ctx) ->
                when(mock.parse()).thenReturn(List.of(entry)))) {

            PipelineContext ctx = buildCtx();
            StepResult result = new ParseTocStep().execute(ctx);

            assertEquals(StepResult.CONTINUE, result);
            assertNotNull(ctx.tocEntries);
            assertEquals(1, ctx.tocEntries.size());
            assertEquals("Chapter One", ctx.tocEntries.get(0).title());
        }
    }

    private PipelineContext buildCtx() {
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubLoader = mockLoader;
        return ctx;
    }
}
