package io.github.vlovric.dazaikindle.unit.pipeline.steps;

import io.github.vlovric.dazaikindle.EpubLoader;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.StepResult;
import io.github.vlovric.dazaikindle.pipeline.steps.LoadEpubStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadEpubStepTest {

    @TempDir
    Path tempDir;

    /** FR02_02-EC_14: non-EPUB file (invalid ZIP) → IOException, pipeline stops. */
    @Test
    void execute_invalidEpubStructure_throwsIOException() throws Exception {
        Path fakeEpub = tempDir.resolve("fake.epub");
        Files.writeString(fakeEpub, "this is not a zip file", StandardCharsets.UTF_8);

        PipelineContext ctx = new PipelineContext(null);
        ctx.bookPath = fakeEpub;

        assertThrows(IOException.class, () -> new LoadEpubStep().execute(ctx));
    }

    /** FR02_02-EC_15: EPUB with missing metadata → null title/author, pipeline continues. */
    @Test
    void execute_missingMetadata_nullTitleContinues() throws Exception {
        Path epubPath = tempDir.resolve("book.epub");
        Files.writeString(epubPath, "placeholder", StandardCharsets.UTF_8);

        try (var mocked = mockConstruction(EpubLoader.class, (mock, ctx) -> {
            doNothing().when(mock).open();
            when(mock.getBookTitle()).thenReturn(null);
            when(mock.getBookAuthor()).thenReturn(null);
        })) {
            PipelineContext ctx = new PipelineContext(null);
            ctx.bookPath = epubPath;

            StepResult result = new LoadEpubStep().execute(ctx);

            assertEquals(StepResult.CONTINUE, result);
            assertNull(ctx.epubTitle);
            assertNull(ctx.epubAuthor);
        }
    }
}
