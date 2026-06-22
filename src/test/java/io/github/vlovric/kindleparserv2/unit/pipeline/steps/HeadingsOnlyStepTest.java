package io.github.vlovric.kindleparserv2.unit.pipeline.steps;

import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;
import io.github.vlovric.kindleparserv2.pipeline.steps.HeadingsOnlyStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeadingsOnlyStepTest {

    @TempDir
    Path tempDir;

    /** FR02_03-HP_01: --headings-only not set → pass-through (CONTINUE, no file written). */
    @Test
    void execute_flagNotSet_continues() throws Exception {
        AppArgs args = argsHeadingsOnly(false, null, null);
        PipelineContext ctx = buildCtx(List.of(heading("Chapter One", 1, 10)), "My Book");

        StepResult result = new HeadingsOnlyStep(args).execute(ctx);

        assertEquals(StepResult.CONTINUE, result);
    }

    /** FR02_03-HP_01: --headings-only with explicit --output → writes to that path, returns FINISH. */
    @Test
    void execute_withExplicitOutput_writesToFile() throws Exception {
        Path out = tempDir.resolve("headings.md");
        AppArgs args = argsHeadingsOnly(true, null, out);
        PipelineContext ctx = buildCtx(List.of(heading("Chapter One", 1, 42)), "My Book");

        StepResult result = new HeadingsOnlyStep(args).execute(ctx);

        assertEquals(StepResult.FINISH, result);
        assertTrue(Files.exists(out));
    }

    /** FR02_03: output filename derived from book title when --output not set. */
    @Test
    void execute_noExplicitOutput_derivesFilenameFromTitle() throws Exception {
        AppArgs args = argsHeadingsOnly(true, null, null);
        PipelineContext ctx = buildCtx(List.of(heading("Chapter One", 1, 42)), "My Book");
        ctx.bookPath = tempDir.resolve("My Book.epub");

        // Execute from tempDir so derived filename lands there
        Path derived = Path.of("My Book_headings.md");
        try {
            StepResult result = new HeadingsOnlyStep(args).execute(ctx);
            assertEquals(StepResult.FINISH, result);
            assertTrue(Files.exists(derived));
        } finally {
            Files.deleteIfExists(derived);
        }
    }

    /** FR02_03: inline markdown contains heading title and location. */
    @Test
    void execute_inlineMarkdown_containsHeadingInfo() throws Exception {
        Path out = tempDir.resolve("out.md");
        AppArgs args = argsHeadingsOnly(true, null, out);
        PipelineContext ctx = buildCtx(
            List.of(heading("The Introduction", 1, 150)),
            "Great Book"
        );

        new HeadingsOnlyStep(args).execute(ctx);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("Great Book"), "Should contain book title");
        assertTrue(content.contains("The Introduction"), "Should contain heading title");
        assertTrue(content.contains("150"), "Should contain location number");
    }

    /** FR02_03: heading level 1 → "##" prefix (title is "#", level-1 headings are "##"). */
    @Test
    void execute_headingLevelMappedToMarkdown() throws Exception {
        Path out = tempDir.resolve("out.md");
        AppArgs args = argsHeadingsOnly(true, null, out);
        PipelineContext ctx = buildCtx(
            List.of(
                heading("Part One", 1, 10),
                heading("Chapter A", 2, 20)
            ),
            "Book"
        );

        new HeadingsOnlyStep(args).execute(ctx);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("## Part One"), "Level 1 heading should use ## prefix");
        assertTrue(content.contains("### Chapter A"), "Level 2 heading should use ### prefix");
    }

    /** FR03_02-HP_01: with --headings-template → uses TemplateRenderer instead of inline markdown. */
    @Test
    void execute_withTemplate_rendersViaTemplate() throws Exception {
        // Create a minimal FreeMarker template file
        Path template = tempDir.resolve("headings.ftl");
        Files.writeString(template,
            "TITLE:${title}\n<#list headings as h>${h.title}:${h.location}\n</#list>",
            StandardCharsets.UTF_8
        );
        Path out = tempDir.resolve("result.txt");
        AppArgs args = argsHeadingsOnly(true, template, out);
        PipelineContext ctx = buildCtx(
            List.of(heading("Prologue", 1, 5)),
            "My Novel"
        );

        new HeadingsOnlyStep(args).execute(ctx);

        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("TITLE:My Novel"), "Template should receive title variable");
        assertTrue(content.contains("Prologue:"), "Template should render heading titles");
    }

    /** FR03_02-EC_01: empty headings list → IOException, pipeline stops. */
    @Test
    void execute_emptyHeadings_throwsIOException() throws Exception {
        Path out = tempDir.resolve("out.md");
        AppArgs args = argsHeadingsOnly(true, null, out);
        PipelineContext ctx = buildCtx(List.of(), "Empty Book");

        assertThrows(java.io.IOException.class, () -> new HeadingsOnlyStep(args).execute(ctx));
        assertFalse(Files.exists(out), "No output file should be created when headings are empty");
    }

    /** FR03_02-EC_02: --headings-template points to non-existent file → IOException. */
    @Test
    void execute_missingTemplateFile_throwsIOException() throws Exception {
        Path missing = tempDir.resolve("nonexistent.ftl");
        Path out = tempDir.resolve("out.txt");
        AppArgs args = argsHeadingsOnly(true, missing, out);
        PipelineContext ctx = buildCtx(List.of(heading("Chapter One", 1, 10)), "My Book");

        assertThrows(java.io.IOException.class, () -> new HeadingsOnlyStep(args).execute(ctx));
    }

    /** Filename special chars in book title are replaced with underscores. */
    @Test
    void execute_specialCharsInTitle_sanitizedInFilename() throws Exception {
        AppArgs args = argsHeadingsOnly(true, null, null);
        PipelineContext ctx = buildCtx(List.of(heading("Ch", 1, 1)), "My/Book:Title");

        Path derived = Path.of("My_Book_Title_headings.md");
        try {
            new HeadingsOnlyStep(args).execute(ctx);
            assertTrue(Files.exists(derived), "Sanitized filename should exist");
        } finally {
            Files.deleteIfExists(derived);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppArgs argsHeadingsOnly(boolean headingsOnly, Path template, Path output) {
        return new AppArgs(
            Path.of("book.epub"), null, "", null,
            output,
            headingsOnly,
            template,
            false, null, null, false
        );
    }

    private static PipelineContext buildCtx(List<Heading> headings, String title) {
        PipelineContext ctx = new PipelineContext(null);
        ctx.resolvedHeadings = headings;
        ctx.epubTitle = title;
        return ctx;
    }

    private static Heading heading(String title, int level, int location) {
        return new Heading(new TocEntry(title, "file.xhtml", null, level), 0, location);
    }
}
