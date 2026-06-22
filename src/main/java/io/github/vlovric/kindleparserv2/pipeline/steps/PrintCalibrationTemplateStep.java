package io.github.vlovric.kindleparserv2.pipeline.steps;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.kindleparser.models.TocEntry;
import io.github.vlovric.kindleparserv2.AppArgs;
import io.github.vlovric.kindleparserv2.pipeline.PipelineContext;
import io.github.vlovric.kindleparserv2.pipeline.PipelineStep;
import io.github.vlovric.kindleparserv2.pipeline.StepResult;

public class PrintCalibrationTemplateStep implements PipelineStep {

    private final AppArgs args;

    public PrintCalibrationTemplateStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        if (args.printCalibrationTemplate() == null) {
            return StepResult.CONTINUE;
        }

        String content = buildCalibrationTemplate(ctx.tocEntries);

        if (ctx.debug != null) {
            ctx.debug.writeText("03_calibration_template_preview.txt", content);
        }

        Path outputPath = args.printCalibrationTemplate();
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        System.out.println("[KindleParser] ✅ Calibration template written to " + outputPath.toAbsolutePath());

        return StepResult.FINISH;
    }

    static String buildCalibrationTemplate(List<TocEntry> tocEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append("# KindleParser calibration template\n");
        sb.append("# Fill in at least 2 locations, keep the rest blank.\n");
        sb.append("# Format: Title - 123\n\n");

        int prevLevel = -1;
        boolean first = true;
        for (TocEntry e : tocEntries) {
            int level = e.level();
            if (!first && (level == 1 || (prevLevel != -1 && level != prevLevel))) {
                sb.append("\n");
            }
            String indent = "  ".repeat(Math.max(0, level - 1));
            sb.append(indent).append(e.title()).append(" - \n");
            prevLevel = level;
            first = false;
        }

        return sb.toString();
    }
}
