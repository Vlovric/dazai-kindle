package io.github.vlovric.dazaikindle.pipeline;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.github.vlovric.dazaikindle.DebugArtifacts;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.pipeline.steps.FitCalibrationStep;
import io.github.vlovric.dazaikindle.pipeline.steps.GroupClippingsStep;
import io.github.vlovric.dazaikindle.pipeline.steps.HeadingsOnlyStep;
import io.github.vlovric.dazaikindle.pipeline.steps.LoadEpubStep;
import io.github.vlovric.dazaikindle.pipeline.steps.ParseClippingsStep;
import io.github.vlovric.dazaikindle.pipeline.steps.ParseTocStep;
import io.github.vlovric.dazaikindle.pipeline.steps.PreprocessBookStep;
import io.github.vlovric.dazaikindle.pipeline.steps.PrintCalibrationTemplateStep;
import io.github.vlovric.dazaikindle.pipeline.steps.RenderOutputStep;
import io.github.vlovric.dazaikindle.pipeline.steps.ResolveHeadingsStep;

/**
 * Main processing pipeline. Composed of a sequence of PipelineSteps, each performing a specific task.
 * The PipelineContext is passed through all steps, allowing them to read and write shared state.
 */
public class Pipeline {

    private final AppArgs args;
    private final Path debugRunDir;

    public Pipeline(AppArgs args) {
        this(args, null);
    }

    /**
     * @param debugRunDir directory to write debug artifacts into when args.debug() is set;
     *                     if null, falls back to a CWD-relative ./debug-runs/run-&lt;timestamp&gt; directory (CLI default)
     */
    public Pipeline(AppArgs args, Path debugRunDir) {
        this.args = args;
        this.debugRunDir = debugRunDir;
    }

    /**
     * Runs the pipeline, executing each step in sequence. If any step returns StepResult.FINISH, the pipeline terminates immediately.
     * @throws Exception
     */
    public PipelineResult run() throws Exception{

        List<PipelineStep> steps = List.of(
            new PreprocessBookStep(args),           // .azw3/.mobi → .epub via Calibre if needed
            new LoadEpubStep(),                     // extract zip, parse OPF/spine/metadata
            new ParseTocStep(),                     // NCX or XHTML TOC → List<TocEntry>
            new PrintCalibrationTemplateStep(args), // [EXIT] if --print-calibration-template
            new FitCalibrationStep(args),           // least-squares fit from calibration file
            new ResolveHeadingsStep(),              // byte offsets → Kindle locations
            new HeadingsOnlyStep(args),             // [EXIT] if --headings-only
            new ParseClippingsStep(args),           // Fyodor subprocess → List<Clipping>
            new GroupClippingsStep(),               // binary-search grouping under headings
            new RenderOutputStep(args)              // FreeMarker → output file
        );

        try (PipelineContext context = buildContext()) {
            for (PipelineStep step : steps) {
                StepResult result = step.execute(context);
                if (result == StepResult.FINISH) {
                    break;
                }
            }
            
            long highlightCount = context.clippings == null ? 0
                    : context.clippings.stream().filter(Clipping::isHighlight).count();

            return new PipelineResult(
                context.matchedBookTitle,
                context.epubAuthor,
                highlightCount
            );
        }
    }

    /**
     * Initializes the PipelineContext, including setting up debug artifacts if --debug is enabled.
     * @return the initialized PipelineContext
     * @throws Exception
     */
    private PipelineContext buildContext() throws Exception {
        DebugArtifacts debug = null;
        if (args.debug()) {
            Path runDir = debugRunDir;
            if (runDir == null) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                runDir = Paths.get("./debug-runs", "run-" + ts);
            }
            Files.createDirectories(runDir);
            debug = new DebugArtifacts(runDir);
            System.out.println("[DazaiKindle] 🧪 Debug run dir: " + runDir.toAbsolutePath());
        }
        return new PipelineContext(debug);
    }
    
}
