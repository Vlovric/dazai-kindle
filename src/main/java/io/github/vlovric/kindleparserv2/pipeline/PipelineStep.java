package io.github.vlovric.kindleparserv2.pipeline;

/**
 * A single step in the processing pipeline. Implementations perform a specific task, reading from and writing to the PipelineContext.
 * The execute method returns a StepResult indicating whether the pipeline should continue to the next step or finish immediately after this step.
 */
public interface PipelineStep {
    StepResult execute(PipelineContext context) throws Exception;
}
