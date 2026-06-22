package io.github.vlovric.kindleparserv2;

import java.io.File;
import java.nio.file.Path;

/**
 * Immutable snapshot of all parsed CLI arguments.
 * Populated once by ArgsParser, then read-only for the entire pipeline.
 */
public record AppArgs(
    Path book,                         // --book          (required)
    Path clippings,                    // --clippings      (required unless headingsOnly or printCalibrationTemplate)
    String titleFilter,                // --title          (optional, default "")
    Path template,                     // --template       (required unless headingsOnly or printCalibrationTemplate)
    Path output,                       // --output         (optional, derived from title if absent)
    boolean headingsOnly,              // --headings-only
    Path headingsTemplate,             // --headings-template (optional)
    boolean debug,                     // --debug
    Path calibrate,                    // --calibrate      (required unless printCalibrationTemplate)
    Path printCalibrationTemplate      // --print-calibration-template
) {
    static AppArgs of(
        File book,
        File clippings,
        String titleFilter,
        File template,
        File output,
        boolean headingsOnly,
        File headingsTemplate,
        boolean debug,
        File calibrate,
        File printCalibrationTemplate
    ) {
        return new AppArgs(
            book.toPath(),
            toPath(clippings),
            titleFilter,
            toPath(template),
            toPath(output),
            headingsOnly,
            toPath(headingsTemplate),
            debug,
            toPath(calibrate),
            toPath(printCalibrationTemplate)
        );
    }

    /**
     * Converts a File to a Path, returning null if the File is null.
     * @param f the File to convert
     * @return the corresponding Path, or null if f is null
     */
    private static Path toPath(File f) {
        return f != null ? f.toPath() : null;
    }
}