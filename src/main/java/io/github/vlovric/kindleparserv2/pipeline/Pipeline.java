package io.github.vlovric.kindleparserv2.pipeline;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.github.vlovric.kindleparser.DebugArtifacts;
import io.github.vlovric.kindleparserv2.AppArgs;

/**
 * Main processing pipeline. Composed of a sequence of PipelineSteps, each performing a specific task.
 * The PipelineContext is passed through all steps, allowing them to read and write shared state.
 */
public class Pipeline {

    private final AppArgs args;

    public Pipeline(AppArgs args) {
        this.args = args;
    }

    /**
     * Runs the pipeline, executing each step in sequence. If any step returns StepResult.FINISH, the pipeline terminates immediately.
     * @throws Exception
     */
    public void run() throws Exception{

        PipelineContext context = buildContext();

        List<PipelineStep> steps = List.of(
            /**
            new PreprocessBookStep(args),       // .azw3/.mobi → .epub via Calibre if needed
            new LoadEpubStep(args),             // extract zip, parse OPF/spine/metadata
            new ParseTocStep(args),             // NCX or XHTML TOC → List<TocEntry>
            new PrintCalibrationTemplateStep(args), // [EXIT] if --print-calibration-template
            new FitCalibrationStep(args),       // least-squares fit from calibration file
            new ResolveHeadingsStep(args),      // byte offsets → Kindle locations
            new HeadingsOnlyStep(args),         // [EXIT] if --headings-only
            new ParseClippingsStep(args),       // Fyodor subprocess → List<Clipping>
            new GroupClippingsStep(args),       // binary-search grouping under headings
            new RenderOutputStep(args)          // FreeMarker → output file
             */
        );

        for (PipelineStep step : steps) {
            StepResult result = step.execute(context);
            if (result == StepResult.FINISH) {
                return;
            }
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
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path runDir = Paths.get("./debug-runs", "run-" + ts);
            Files.createDirectories(runDir);
            debug = new DebugArtifacts(runDir);
            System.out.println("[KindleParser] 🧪 Debug run dir: " + runDir.toAbsolutePath());
        }
        return new PipelineContext(debug);
    }
    
}
