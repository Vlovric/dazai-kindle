package io.github.vlovric.kindleparser.pipeline.steps;

import java.util.Map;

import io.github.vlovric.kindleparser.calibration.CalibrationFit;
import io.github.vlovric.kindleparser.calibration.CalibrationFitter;
import io.github.vlovric.kindleparser.AppArgs;
import io.github.vlovric.kindleparser.pipeline.PipelineContext;
import io.github.vlovric.kindleparser.pipeline.PipelineStep;
import io.github.vlovric.kindleparser.pipeline.StepResult;

/**
 * Reads the user-supplied calibration file, performs a least-squares linear fit between
 * byte offsets and Kindle locations, and stores the result in {@link PipelineContext}.
 * The fit coefficients ({@code bytesPerLocation} and {@code locationBias}) are what make
 * location resolution accurate — without calibration, the 128-bytes-per-location default
 * drifts by tens or hundreds of locations across a book (see Agent Insights §2).
 */
public class FitCalibrationStep implements PipelineStep {

    /** Warn when RMSE exceeds this many locations — empirically, above ~5 the grouping becomes noticeably wrong. */
    private static final double RMSE_WARNING_THRESHOLD = 5.0;

    private final AppArgs args;

    public FitCalibrationStep(AppArgs args) {
        this.args = args;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        CalibrationFitter fitter = new CalibrationFitter();

        Map<String, Integer> targets = fitter.parseCalibrationFile(args.calibrate());
        CalibrationFit fit = fitter.fit(targets, ctx.epubLoader, ctx.tocEntries);

        System.out.printf(
            "[KindleParser] 📐 Calibration: bytesPerLocation=%.2f, bias=%.2f, points=%d, rmse=%.2f locations%n",
            fit.bytesPerLocation(), fit.locationBias(), fit.pointsUsed(), fit.rmseLocations()
        );

        if (fit.rmseLocations() > RMSE_WARNING_THRESHOLD) {
            System.out.printf(
                "[KindleParser] ⚠️  High calibration RMSE (%.2f > %.1f). Results may be inaccurate — add more calibration points.%n",
                fit.rmseLocations(), RMSE_WARNING_THRESHOLD
            );
        }

        ctx.bytesPerLocation = fit.bytesPerLocation();
        ctx.locationBias     = fit.locationBias();

        return StepResult.CONTINUE;
    }
}
