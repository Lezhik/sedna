package io.sedna.runtime.execution;

/** Deterministic runtime execution options (test hooks and resume). */
public record RuntimeExecutionOptions(
    long injectFailureAfterNodeId, int resumeAfterNodes, String resumeFsmState) {

  public static final RuntimeExecutionOptions DEFAULT =
      new RuntimeExecutionOptions(0L, 0, "INIT");

  public RuntimeExecutionOptions {
    resumeFsmState = resumeFsmState == null ? "INIT" : resumeFsmState;
  }

  public static RuntimeExecutionOptions injectFailure(long nodeId) {
    return new RuntimeExecutionOptions(nodeId, 0, "INIT");
  }

  public static RuntimeExecutionOptions resume(int completedNodes, String fsmState) {
    return new RuntimeExecutionOptions(0L, completedNodes, fsmState);
  }
}
