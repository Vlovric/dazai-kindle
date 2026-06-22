package io.github.vlovric.kindleparserv2.pipeline;

import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.kindleparser.DebugArtifacts;
import io.github.vlovric.kindleparser.models.Clipping;
import io.github.vlovric.kindleparser.models.Heading;
import io.github.vlovric.kindleparser.models.HeadingGroup;
import io.github.vlovric.kindleparser.models.TocEntry;

/**
 * Mutable bag of state passed through all pipeline steps.
 * Fields are populated progressively — each step reads what it needs
 * and writes what it produces. Null means "not yet populated".
 */
public class PipelineContext {
    
    /**
     * Populated by: PreprocessBookStep
     */
    public Path bookPath;

    /**
     * Populated by: LoadEpubStep
     */
    public String epubTitle;

    /**
     * Populated by: LoadEpubStep
     */
    public String epubAuthor;

    /**
     * Populated by: ParseTocStep
     */
    public List<TocEntry> tocEntries;

    /**
     * Populated by: FitCalibrationStep
     */
    public double bytesPerLocation;

    /**
     * Populated by: FitCalibrationStep
     */
    public double locationBias;

    /**
     * Populated by: ResolveHeadingsStep
     */
    public List<Heading> resolvedHeadings;

    /**
     * Populated by: ParseClippingsStep
     */
    public List<Clipping> clippings;

    /**
     * Populated by: ParseClippingsStep
     */
    public String matchedBookTitle;

    /**
     * Populated by: GroupClippingsStep
     */
    public List<HeadingGroup> groups;

    /**
     * Populated by: Pipeline constructor (null if --debug not set)
     */
    public final DebugArtifacts debug;

    public PipelineContext(DebugArtifacts debug) {
        this.debug = debug;
    }

}
