package io.github.vlovric.kindleparserv2.unit.pipeline.steps;

import io.github.vlovric.kindleparser.fyodor.FyodorClippingsParser;
import io.github.vlovric.kindleparser.fyodor.FyodorParseResult;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;
import io.github.vlovric.kindleparserv2.pipeline.steps.ParseClippingsStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParseClippingsStepTest {

    @TempDir
    Path tempDir;

    /** FR01_01-EC_10: no EPUB title and no title filter → cannot identify book, throws IOException. */
    @Test
    void execute_noEpubTitleAndNoFilter_throwsIOException() {
        AppArgs args = args(null, "");
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubTitle = null;

        assertThrows(IOException.class, () -> new ParseClippingsStep(args).execute(ctx));
    }

    /** FR01_01-EC_10: blank EPUB title and blank filter → same error. */
    @Test
    void execute_blankEpubTitleAndBlankFilter_throwsIOException() {
        AppArgs args = args(null, "   ");
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubTitle = "  ";

        assertThrows(IOException.class, () -> new ParseClippingsStep(args).execute(ctx));
    }

    /** FR01_01-EC_05 / EC_14: Fyodor returns no clippings → throws IOException. */
    @Test
    void execute_emptyClippings_throwsIOException() throws Exception {
        Path clippingsFile = tempDir.resolve("clippings.txt");
        AppArgs args = args(clippingsFile, "MyBook");

        FyodorParseResult emptyResult = new FyodorParseResult(List.of(), null, null, null);

        try (var mocked = mockConstruction(FyodorClippingsParser.class, (mock, ctx) ->
                when(mock.parse(eq("MyBook"), isNull(), isNull(), isNull())).thenReturn(emptyResult))) {

            PipelineContext ctx = new PipelineContext(null);
            ctx.epubTitle = null;

            assertThrows(IOException.class, () -> new ParseClippingsStep(args).execute(ctx));
        }
    }

    /** FR01_01-HP_01: valid parse with title filter → context populated, returns CONTINUE. */
    @Test
    void execute_validParse_populatesContextAndContinues() throws Exception {
        Path clippingsFile = tempDir.resolve("clippings.txt");
        AppArgs args = args(clippingsFile, "MyBook");

        Clipping clipping = new Clipping("MyBook", "Author", "highlight", "100", null, "2024", "some text");
        FyodorParseResult result = new FyodorParseResult(List.of(clipping), null, null, "MyBook");

        try (var mocked = mockConstruction(FyodorClippingsParser.class, (mock, ctx) ->
                when(mock.parse(eq("MyBook"), isNull(), isNull(), isNull())).thenReturn(result))) {

            PipelineContext ctx = new PipelineContext(null);
            ctx.epubTitle = null;

            StepResult stepResult = new ParseClippingsStep(args).execute(ctx);

            assertEquals(StepResult.CONTINUE, stepResult);
            assertNotNull(ctx.clippings);
            assertEquals(1, ctx.clippings.size());
            assertEquals("MyBook", ctx.matchedBookTitle);
        }
    }

    /** FR01_01-HP_03: no title filter → epubTitle passed to parser as expected book title. */
    @Test
    void execute_noFilter_passesEpubTitleToParser() throws Exception {
        Path clippingsFile = tempDir.resolve("clippings.txt");
        AppArgs args = args(clippingsFile, "");

        Clipping clipping = new Clipping("Book From Epub", "Author", "highlight", "50", null, "2024", "text");
        FyodorParseResult result = new FyodorParseResult(List.of(clipping), null, null, "Book From Epub");

        try (var mocked = mockConstruction(FyodorClippingsParser.class, (mock, ctx) ->
                when(mock.parse(eq(""), eq("Book From Epub"), isNull(), isNull())).thenReturn(result))) {

            PipelineContext ctx = new PipelineContext(null);
            ctx.epubTitle = "Book From Epub";

            StepResult stepResult = new ParseClippingsStep(args).execute(ctx);

            assertEquals(StepResult.CONTINUE, stepResult);
            assertEquals("Book From Epub", ctx.matchedBookTitle);
        }
    }

    /** FR01_01-HP_01: matched book title falls back to first clipping's bookTitle when selectedBookTitle is null. */
    @Test
    void execute_nullSelectedBookTitle_fallsBackToClippingBookTitle() throws Exception {
        Path clippingsFile = tempDir.resolve("clippings.txt");
        AppArgs args = args(clippingsFile, "filter");

        Clipping clipping = new Clipping("Fallback Title", "Author", "note", "200", null, "2024", "note text");
        FyodorParseResult result = new FyodorParseResult(List.of(clipping), null, null, null);

        try (var mocked = mockConstruction(FyodorClippingsParser.class, (mock, ctx) ->
                when(mock.parse(eq("filter"), isNull(), isNull(), isNull())).thenReturn(result))) {

            PipelineContext ctx = new PipelineContext(null);
            ctx.epubTitle = null;

            new ParseClippingsStep(args).execute(ctx);

            assertEquals("Fallback Title", ctx.matchedBookTitle);
        }
    }

    private AppArgs args(Path clippingsFile, String titleFilter) {
        return new AppArgs(
                Path.of("book.epub"),
                clippingsFile,
                titleFilter,
                null,
                null,
                false,
                null,
                false,
                Path.of("calibration.txt"),
                null,
                false
        );
    }
}
