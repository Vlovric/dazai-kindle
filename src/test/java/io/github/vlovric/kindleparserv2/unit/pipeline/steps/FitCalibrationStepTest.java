package io.github.vlovric.kindleparserv2.unit.pipeline.steps;

import io.github.vlovric.kindleparser.EpubLoader;
import io.github.vlovric.kindleparser.calibration.CalibrationFit;
import io.github.vlovric.kindleparser.calibration.CalibrationFitter;
import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;
import io.github.vlovric.kindleparserv2.pipeline.steps.FitCalibrationStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FitCalibrationStepTest {

    @TempDir
    Path tempDir;

    @Mock
    EpubLoader mockLoader;

    /** FR02_02-HP_01: successful fit → ctx populated with bytesPerLocation and locationBias, returns CONTINUE. */
    @Test
    void execute_successfulFit_populatesContextAndContinues() throws Exception {
        Path calibFile = tempDir.resolve("calibration.txt");
        Files.writeString(calibFile, "Chapter One - 100\nChapter Two - 200\n", StandardCharsets.UTF_8);

        CalibrationFit fakeFit = new CalibrationFit(128.0, 0.0, 2, 0.5);

        try (var mocked = mockConstruction(CalibrationFitter.class, (mock, ctx) -> {
            when(mock.parseCalibrationFile(calibFile)).thenReturn(Map.of("Chapter One", 100, "Chapter Two", 200));
            when(mock.fit(any(), eq(mockLoader), any())).thenReturn(fakeFit);
        })) {
            AppArgs args = argsWithCalibrate(calibFile);
            PipelineContext ctx = buildCtx();

            StepResult result = new FitCalibrationStep(args).execute(ctx);

            assertEquals(StepResult.CONTINUE, result);
            assertEquals(128.0, ctx.bytesPerLocation, 0.001);
            assertEquals(0.0, ctx.locationBias, 0.001);
        }
    }

    /** FR02_02-EC_13: RMSE > 5 → warning printed to stdout, pipeline continues (no exception). */
    @Test
    void execute_highRmse_warnsButContinues() throws Exception {
        Path calibFile = tempDir.resolve("calibration.txt");
        Files.writeString(calibFile, "Chapter One - 100\nChapter Two - 200\n", StandardCharsets.UTF_8);

        CalibrationFit highRmseFit = new CalibrationFit(128.0, 0.0, 2, 8.5);

        try (var mocked = mockConstruction(CalibrationFitter.class, (mock, ctx) -> {
            when(mock.parseCalibrationFile(calibFile)).thenReturn(Map.of("Chapter One", 100, "Chapter Two", 200));
            when(mock.fit(any(), eq(mockLoader), any())).thenReturn(highRmseFit);
        })) {
            AppArgs args = argsWithCalibrate(calibFile);
            PipelineContext ctx = buildCtx();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream original = System.out;
            System.setOut(new PrintStream(out));
            StepResult result;
            try {
                result = new FitCalibrationStep(args).execute(ctx);
            } finally {
                System.setOut(original);
            }

            assertEquals(StepResult.CONTINUE, result);
            String output = out.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("⚠") || output.toLowerCase().contains("rmse") || output.contains("8.50"),
                "Expected RMSE warning in stdout, got: " + output);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppArgs argsWithCalibrate(Path calibFile) {
        return new AppArgs(
            Path.of("book.epub"), null, "", null, null,
            false, null, false, calibFile, null
        );
    }

    private PipelineContext buildCtx() {
        PipelineContext ctx = new PipelineContext(null);
        ctx.epubLoader = mockLoader;
        ctx.tocEntries = List.of(new TocEntry("Chapter One", "ch1.xhtml", null, 1));
        return ctx;
    }
}
