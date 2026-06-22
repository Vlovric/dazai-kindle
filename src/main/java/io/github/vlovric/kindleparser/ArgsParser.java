package io.github.vlovric.kindleparser;

import java.io.File;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Holds args4j annotations
 * Parses CLI arguments into an AppArgs record.
 * Validates cross-argument constraints before returning the AppArgs instance.
 */
public class ArgsParser {
    
    @Option(name = "--book", required = true, usage = "Path to .epub/.azw3/.mobi file")
    private File book;

    @Option(name = "--clippings", usage = "Path to MyClippings.txt from your Kindle")
    private File clippings;

    @Option(name = "--title", usage = "Book title substring to filter clippings (case-insensitive)")
    private String title = "";

    @Option(name = "--template", usage = "Path to the FreeMarker template file (.ftl)")
    private File template;

    @Option(name = "--output", usage = "Output file path. Defaults to <Book Title>.md")
    private File output;

    @Option(name = "--headings-only", usage = "Only extract and print the Table of Contents")
    private boolean headingsOnly = false;

    @Option(name = "--headings-template", usage = "FreeMarker template for headings-only output")
    private File headingsTemplate;

    @Option(name = "--debug", usage = "Write intermediate artifacts to a debug run directory")
    private boolean debug = false;

    @Option(name = "--calibrate", usage = "Path to calibration file (e.g. 'Heading - 123')")
    private File calibrate;

    @Option(name = "--print-calibration-template", usage = "Write calibration template to path, then exit")
    private File printCalibrationTemplate;

    @Option(name = "--overwrite-fyodor-template", usage = "Overwrite ~/.config/fyodor/template.erb even if it differs from the bundled template")
    private boolean overwriteFyodorTemplate = false;

    /**
     * Parses args and validates cross-argument constraints.
     * Throws CmdLineException for structural parse failures.
     * Throws IllegalArgumentException for semantic validation failures.
     */
    public AppArgs parse(String[] args) throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);
        validate();
        return buildConfig();
    }

    private void validate() {
        boolean isPrinting = printCalibrationTemplate != null;

        if (!isPrinting && calibrate == null) {
            throw new IllegalArgumentException(
                "[KindleParser] ❌ Calibration is mandatory.\n" +
                "[KindleParser]    Provide --calibrate <file>, or run --print-calibration-template <path>."
            );
        }
        if (!isPrinting && calibrate != null && !calibrate.exists()) {
            throw new IllegalArgumentException(
                "[KindleParser] ❌ Calibration file not found: " + calibrate.getPath()
            );
        }
        if (!isPrinting && !headingsOnly) {
            if (clippings == null) {
                throw new IllegalArgumentException("[KindleParser] ❌ Missing --clippings.");
            }
            if (template == null) {
                throw new IllegalArgumentException("[KindleParser] ❌ Missing --template.");
            }
        }
    }

    /**
     * Builds the AppArgs record from the parsed fields.
     * @return the AppArgs instance to be used for the pipeline
     */
    private AppArgs buildConfig() {
        return AppArgs.of(
            book,
            clippings,
            title,
            template,
            output,
            headingsOnly,
            headingsTemplate,
            debug,
            calibrate,
            printCalibrationTemplate,
            overwriteFyodorTemplate
        );
    }

}
