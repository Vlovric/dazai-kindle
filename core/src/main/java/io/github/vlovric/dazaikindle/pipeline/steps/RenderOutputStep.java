package io.github.vlovric.dazaikindle.pipeline.steps;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.vlovric.dazaikindle.TemplateRenderer;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.pipeline.PipelineContext;
import io.github.vlovric.dazaikindle.pipeline.PipelineStep;
import io.github.vlovric.dazaikindle.pipeline.StepResult;

/**
 * Renders grouped clippings through the user's FreeMarker template and writes the output file.
 */
public class RenderOutputStep implements PipelineStep {

    private final AppArgs args;

    public RenderOutputStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        String title = resolveTitle(ctx);
        String author = resolveAuthor(ctx);
        Path outputPath = resolveOutputPath(title, author);

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        System.out.println("[DazaiKindle] 📝 Rendering output...");
        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            new TemplateRenderer(args.template()).render(ctx.groups, title, writer);
        }

        System.out.println("[DazaiKindle] ✅ Saved to " + outputPath.toAbsolutePath());
        System.out.println("[DazaiKindle] ✅ Done.");

        return StepResult.FINISH;
    }

    /** Priority: matched book title from Fyodor → --title filter → book filename stem. */
    private String resolveTitle(PipelineContext ctx) {
        if (ctx.matchedBookTitle != null && !ctx.matchedBookTitle.isBlank()) {
            return ctx.matchedBookTitle;
        }
        if (args.titleFilter() != null && !args.titleFilter().isBlank()) {
            return args.titleFilter();
        }
        if (ctx.bookPath != null) {
            String name = ctx.bookPath.getFileName().toString();
            int dot = name.lastIndexOf('.');
            return dot == -1 ? name : name.substring(0, dot);
        }
        return "Output";
    }

    /** Returns the EPUB author for the filename, or null if unavailable (falls back to title-only). */
    private String resolveAuthor(PipelineContext ctx) {
        return (ctx.epubAuthor != null && !ctx.epubAuthor.isBlank()) ? ctx.epubAuthor : null;
    }

    /**
     * Builds "title_author.md" (or just "title.md" if the author couldn't be
     * resolved) under outputDir, unless an explicit --output path was given.
     */
    private Path resolveOutputPath(String title, String author) {
        if (args.output() != null) {
            return args.output();
        }
        String baseName = sanitize(title);
        if (author != null) {
            baseName += "_" + sanitize(author);
        }
        Path fileName = Path.of(baseName + ".md");
        return args.outputDir() != null ? args.outputDir().resolve(fileName) : fileName;
    }

    /** Strips characters illegal in filenames on Windows/macOS/Linux so the path is safe on all platforms. */
    private static String sanitize(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
