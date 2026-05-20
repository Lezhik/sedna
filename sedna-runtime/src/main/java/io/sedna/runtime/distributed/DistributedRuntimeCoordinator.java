package io.sedna.runtime.distributed;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;

/** Distributed runtime coordinator (Phase 14 prototype). */
public interface DistributedRuntimeCoordinator {

  Result<ExecutionTrace, SemanticError> execute(RuntimeExecutionPlan plan, RuntimeExecutionOptions options);
}
