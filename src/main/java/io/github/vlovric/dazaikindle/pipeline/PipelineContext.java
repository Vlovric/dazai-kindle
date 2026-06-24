package io.github.vlovric.dazaikindle.pipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.github.vlovric.dazaikindle.DebugArtifacts;
import io.github.vlovric.dazaikindle.EpubLoader;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.models.Heading;
import io.github.vlovric.dazaikindle.models.HeadingGroup;
import io.github.vlovric.dazaikindle.models.TocEntry;

/**
 * Mutable bag of state passed through all pipeline steps.
 * Fields are populated progressively — each step reads what it needs
 * and writes what it produces. Null means "not yet populated".
 *
 * Implements AutoCloseable to ensure EpubLoader is always cleaned up, even on exception.
 */
public class PipelineContext implements AutoCloseable {
    
    /**
     * Populated by: PreprocessBookStep
     */
    public Path bookPath;

    /**
     * Populated by: LoadEpubStep. Kept open until ResolveHeadingsStep completes.
     * Closed automatically when PipelineContext is closed.
     */
    public EpubLoader epubLoader;

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

    @Override
    public void close() throws IOException {
        if (epubLoader != null) {
            epubLoader.close();
        }
    }

}
