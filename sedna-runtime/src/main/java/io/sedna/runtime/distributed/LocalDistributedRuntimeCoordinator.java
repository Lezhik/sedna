package io.sedna.runtime.distributed;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.execution.ProfileRuntimeExecutor;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;

/** Single-node fallback coordinator (deterministic local execution). */
public final class LocalDistributedRuntimeCoordinator implements DistributedRuntimeCoordinator {

  private final ProfileRuntimeExecutor executor = new ProfileRuntimeExecutor();

  @Override
  public Result<ExecutionTrace, SemanticError> execute(
      RuntimeExecutionPlan plan, RuntimeExecutionOptions options) {
    return executor.execute(plan, options);
  }
}
