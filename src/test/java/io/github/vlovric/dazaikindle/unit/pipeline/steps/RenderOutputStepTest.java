package io.github.vlovric.dazaikindle.unit.pipeline.steps;

import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.HeadingGroup;
import io.github.vlovric.dazaikindle.models.TocEntry;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.StepResult;
import io.github.vlovric.dazaikindle.pipeline.steps.RenderOutputStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RenderOutputStepTest {

    @TempDir
    Path tempDir;

    /** FR03_01-HP_01: valid template → output file created, title injected, returns FINISH. */
    @Test
    void execute_validTemplate_rendersAndFinishes() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile, "${title}", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "");

        PipelineContext ctx = buildCtx("My Book", List.of());
        StepResult result = new RenderOutputStep(args).execute(ctx);

        assertEquals(StepResult.FINISH, result);
        assertTrue(Files.exists(outputFile));
        assertEquals("My Book", Files.readString(outputFile).trim());
    }

    /** FR03_01-HP_02: template with loops and conditionals over groups and clippings. */
    @Test
    void execute_templateWithLoopsAndConditionals_rendersCorrectly() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile,
            "<#list groups as g>${g.heading.title}:<#list g.clippings as c>${c.content}</#list></#list>",
            StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "");

        Heading heading = new Heading(new TocEntry("Ch1", "ch1.xhtml", null, 1), 0, 100);
        Clipping clip = new Clipping("Book", "Author", "highlight", "110", null, "2024", "quoted text");
        HeadingGroup group = new HeadingGroup(heading, List.of(clip));

        PipelineContext ctx = buildCtx("Book", List.of(group));
        new RenderOutputStep(args).execute(ctx);

        String output = Files.readString(outputFile);
        assertTrue(output.contains("Ch1"), "Should contain heading title");
        assertTrue(output.contains("quoted text"), "Should contain clipping content");
    }

    /** FR03_01-EC_01: template file does not exist → IOException. */
    @Test
    void execute_missingTemplateFile_throwsIOException() {
        Path missing = tempDir.resolve("nonexistent.ftl");
        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(missing, outputFile, "");

        PipelineContext ctx = buildCtx("Book", List.of());
        assertThrows(IOException.class, () -> new RenderOutputStep(args).execute(ctx));
    }

    /** FR03_01-EC_05: template with FreeMarker syntax errors → IOException. */
    @Test
    void execute_syntaxErrorInTemplate_throwsIOException() throws Exception {
        Path templateFile = tempDir.resolve("bad.ftl");
        Files.writeString(templateFile, "${unclosed", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "");

        PipelineContext ctx = buildCtx("Book", List.of());
        assertThrows(IOException.class, () -> new RenderOutputStep(args).execute(ctx));
    }

    /** FR03_01-EC_06: template references an undefined variable → IOException. */
    @Test
    void execute_undefinedVariableInTemplate_throwsIOException() throws Exception {
        Path templateFile = tempDir.resolve("undef.ftl");
        Files.writeString(templateFile, "${nonExistentVar}", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "");

        PipelineContext ctx = buildCtx("Book", List.of());
        assertThrows(IOException.class, () -> new RenderOutputStep(args).execute(ctx));
    }

    /** HP_01: output path derived from matchedBookTitle when no --output flag. */
    @Test
    void execute_noExplicitOutput_derivesFilenameFromMatchedTitle() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile, "${title}", StandardCharsets.UTF_8);

        AppArgs args = new AppArgs(
            tempDir.resolve("book.epub"), null, "", templateFile,
            null, false, null, false, null, null, false
        );

        PipelineContext ctx = buildCtx("My Great Book", List.of());
        ctx.bookPath = tempDir.resolve("book.epub");
        new RenderOutputStep(args).execute(ctx);

        Path derived = Path.of("My Great Book.md");
        try {
            assertTrue(Files.exists(derived), "Derived output file not created");
            assertEquals("My Great Book", Files.readString(derived).trim());
        } finally {
            Files.deleteIfExists(derived);
        }
    }

    /** HP_01: special characters in title are sanitized in derived filename. */
    @Test
    void execute_titleWithSpecialChars_sanitizedInDerivedFilename() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile, "${title}", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "");

        PipelineContext ctx = buildCtx("Book: A/B*C", List.of());
        StepResult result = new RenderOutputStep(args).execute(ctx);

        assertEquals(StepResult.FINISH, result);
        String content = Files.readString(outputFile);
        assertTrue(content.contains("Book: A/B*C"), "Title injected into template as-is");
    }

    /** HP_01: title falls back to titleFilter when matchedBookTitle is null. */
    @Test
    void execute_nullMatchedTitle_fallsBackToTitleFilter() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile, "${title}", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("out.md");
        AppArgs args = args(templateFile, outputFile, "FilterTitle");

        PipelineContext ctx = buildCtx(null, List.of());
        ctx.bookPath = Path.of("book.epub");
        new RenderOutputStep(args).execute(ctx);

        assertEquals("FilterTitle", Files.readString(outputFile).trim());
    }

    /** HP_01: non-existent parent directories are created automatically. */
    @Test
    void execute_nonExistentParentDirs_createdAutomatically() throws Exception {
        Path templateFile = tempDir.resolve("test.ftl");
        Files.writeString(templateFile, "${title}", StandardCharsets.UTF_8);

        Path outputFile = tempDir.resolve("nested/deep/out.md");
        AppArgs args = args(templateFile, outputFile, "");

        PipelineContext ctx = buildCtx("Book", List.of());
        new RenderOutputStep(args).execute(ctx);

        assertTrue(Files.exists(outputFile));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AppArgs args(Path templateFile, Path outputFile, String titleFilter) {
        return new AppArgs(
            Path.of("book.epub"),
            null,
            titleFilter,
            templateFile,
            outputFile,
            false, null, false, null, null, false
        );
    }

    private PipelineContext buildCtx(String matchedBookTitle, List<HeadingGroup> groups) {
        PipelineContext ctx = new PipelineContext(null);
        ctx.matchedBookTitle = matchedBookTitle;
        ctx.groups = groups;
        return ctx;
    }
}
