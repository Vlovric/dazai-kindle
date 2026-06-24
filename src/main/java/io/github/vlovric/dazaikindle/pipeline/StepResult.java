package io.github.vlovric.dazaikindle.pipeline;

/**
 * Return type of PipelineStep.execute, indicating whether to continue to the next step or finish immediately after the step.
 */
public enum StepResult {
    CONTINUE,
    FINISH
}
