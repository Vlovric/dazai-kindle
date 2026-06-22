package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.kindleparser.TemplateRenderer;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

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
        String title = ctx.epubTitle != null ? ctx.epubTitle : deriveTitle(ctx);

        Path outputPath = resolveOutputPath(args, title);

        if (args.headingsTemplate() != null) {
            renderWithTemplate(headings, title, args.headingsTemplate(), outputPath);
        } else {
            renderMarkdown(headings, title, outputPath);
        }

        System.out.println("[KindleParser] ✅ Headings written to " + outputPath.toAbsolutePath());
        return StepResult.FINISH;
    }

    private static void renderWithTemplate(
        List<Heading> headings,
        String title,
        Path templatePath,
        Path outputPath
    ) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            new TemplateRenderer(templatePath).renderHeadings(headings, title, writer);
        }
    }

    private static void renderMarkdown(List<Heading> headings, String title, Path outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        for (Heading h : headings) {
            String prefix = "#".repeat(Math.min(h.level() + 1, 6));
            sb.append(prefix).append(" ").append(h.title())
              .append("  *(Location: ").append(h.location()).append(")*\n");
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }

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
