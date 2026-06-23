package io.github.vlovric.kindleparser.pipeline.steps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.kindleparser.TemplateRenderer;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.AppArgs;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.PipelineStep;
import io.github.vlovric.kindleparser.pipeline.StepResult;

/**
 * Writes resolved headings with their Kindle locations to a file, then finishes the pipeline.
 * When {@code --headings-only} is not set, this step is a no-op and the pipeline continues.
 * Note: calibration is still required even for this step — {@link ResolveHeadingsStep} depends
 * on the fit from {@link FitCalibrationStep} and runs before this step regardless (see Agent Insights §11).
 */
public class HeadingsOnlyStep implements PipelineStep {

    private final AppArgs args;

    public HeadingsOnlyStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        if (!args.headingsOnly()) {
            return StepResult.CONTINUE;
        }

        List<Heading> headings = ctx.resolvedHeadings;
        if (headings == null || headings.isEmpty()) {
            throw new IOException("[KindleParser] ❌ No headings were resolved. Cannot write headings-only output.");
        }

        String title = ctx.epubTitle != null ? ctx.epubTitle : deriveTitle(ctx);
        Path outputPath = resolveOutputPath(args, title);
        Path templatePath = resolveTemplate(args.headingsTemplate());
        boolean usingBundled = args.headingsTemplate() == null;

        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            new TemplateRenderer(templatePath).renderHeadings(headings, title, writer);
        } finally {
            if (usingBundled) {
                Files.deleteIfExists(templatePath);
            }
        }

        System.out.println("[KindleParser] ✅ Headings written to " + outputPath.toAbsolutePath());
        return StepResult.FINISH;
    }

    /**
     * Returns the user-provided template path, or extracts the bundled {@code headings_default.ftl}
     * to a temp file. The caller is responsible for deleting the temp file when done.
     */
    private static Path resolveTemplate(Path userTemplate) throws IOException {
        if (userTemplate != null) {
            return userTemplate;
        }
        Path tmp = Files.createTempFile("headings_default_", ".ftl");
        try (InputStream is = HeadingsOnlyStep.class.getResourceAsStream("/headings_default.ftl")) {
            if (is == null) throw new IOException("Could not find /headings_default.ftl in resources");
            Files.write(tmp, is.readAllBytes());
        }
        return tmp;
    }

    /** Strips characters illegal in filenames on Windows/macOS/Linux so the path is safe on all platforms. */
    private static Path resolveOutputPath(AppArgs args, String title) {
        if (args.output() != null) {
            return args.output();
        }
        String baseName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        return Path.of(baseName + "_headings.md");
    }

    private static String deriveTitle(PipelineContext ctx) {
        if (ctx.bookPath != null) {
            String name = ctx.bookPath.getFileName().toString();
            int dot = name.lastIndexOf('.');
            return dot == -1 ? name : name.substring(0, dot);
        }
        return "Headings";
    }
}
