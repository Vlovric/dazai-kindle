package io.github.vlovric.kindleparserv2.unit.pipeline.steps;

import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;
import io.github.vlovric.kindleparserv2.pipeline.steps.PrintCalibrationTemplateStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrintCalibrationTemplateStepTest {

    @TempDir
    Path tempDir;

    /** FR04_01-HP_01: flag not set → pass-through (CONTINUE, no file written). */
    @Test
    void execute_flagNotSet_continues() throws Exception {
        AppArgs args = argsWithTemplate(null);
        PipelineContext ctx = buildCtx(List.of(tocEntry("Chapter One", 1)));

        StepResult result = new PrintCalibrationTemplateStep(args).execute(ctx);

        assertEquals(StepResult.CONTINUE, result);
    }

    /** FR04_01-HP_01: flag set → file written, returns FINISH. */
    @Test
    void execute_flagSet_writesFileAndFinishes() throws Exception {
        Path out = tempDir.resolve("calibration.txt");
        AppArgs args = argsWithTemplate(out);
        PipelineContext ctx = buildCtx(List.of(
            tocEntry("Introduction", 1),
            tocEntry("Chapter One", 1),
            tocEntry("Chapter Two", 1)
        ));

        StepResult result = new PrintCalibrationTemplateStep(args).execute(ctx);

        assertEquals(StepResult.FINISH, result);
        assertTrue(Files.exists(out));
        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("Introduction - "));
        assertTrue(content.contains("Chapter One - "));
        assertTrue(content.contains("Chapter Two - "));
    }

    /** FR04_01: template includes comment header lines. */
    @Test
    void execute_outputContainsCommentHeader() throws Exception {
        Path out = tempDir.resolve("calibration.txt");
        AppArgs args = argsWithTemplate(out);
        PipelineContext ctx = buildCtx(List.of(tocEntry("Preface", 1)));

        new PrintCalibrationTemplateStep(args).execute(ctx);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("#"), "Expected comment header at top of file");
    }

    /** FR04_01: sub-headings (level 2+) are indented. */
    @Test
    void execute_subHeadingsIndented() throws Exception {
        Path out = tempDir.resolve("calibration.txt");
        AppArgs args = argsWithTemplate(out);
        PipelineContext ctx = buildCtx(List.of(
            tocEntry("Part One", 1),
            tocEntry("Chapter 1", 2),
            tocEntry("Section 1.1", 3)
        ));

        new PrintCalibrationTemplateStep(args).execute(ctx);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        String[] lines = content.lines().filter(l -> !l.startsWith("#") && !l.isBlank()).toArray(String[]::new);
        assertFalse(lines[0].startsWith(" "), "Level 1 should not be indented");
        assertTrue(lines[1].startsWith("  "), "Level 2 should be indented 2 spaces");
        assertTrue(lines[2].startsWith("    "), "Level 3 should be indented 4 spaces");
    }

    /** FR04_01-EC_01: empty TOC → file written with header only (no entries). */
    @Test
    void execute_emptyToc_writesHeaderOnly() throws Exception {
        Path out = tempDir.resolve("calibration.txt");
        AppArgs args = argsWithTemplate(out);
        PipelineContext ctx = buildCtx(List.of());

        StepResult result = new PrintCalibrationTemplateStep(args).execute(ctx);

        assertEquals(StepResult.FINISH, result);
        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("#"));
        // No entry lines beyond the header comments
        long entryLines = content.lines()
            .filter(l -> !l.startsWith("#") && !l.isBlank())
            .count();
        assertEquals(0, entryLines);
    }

    /** FR04_01-EC_02: output path in non-existent directory → directories created automatically. */
    @Test
    void execute_nonExistentParentDir_directoriesCreated() throws Exception {
        Path out = tempDir.resolve("nested/deep/calibration.txt");
        AppArgs args = argsWithTemplate(out);
        PipelineContext ctx = buildCtx(List.of(tocEntry("Chapter", 1)));

        new PrintCalibrationTemplateStep(args).execute(ctx);

        assertTrue(Files.exists(out), "File should be created in newly created directories");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppArgs argsWithTemplate(Path outputPath) {
        return new AppArgs(
            Path.of("book.epub"), null, "", null, null,
            false, null, false, null,
            outputPath, false
        );
    }

    private static PipelineContext buildCtx(List<TocEntry> entries) {
        PipelineContext ctx = new PipelineContext(null);
        ctx.tocEntries = entries;
        return ctx;
    }

    private static TocEntry tocEntry(String title, int level) {
        return new TocEntry(title, "file.xhtml", null, level);
    }
}
