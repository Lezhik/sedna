package io.sedna.runtime;

import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionOrdering;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import java.util.List;

/** DAG-only runtime scheduler (MVP). */
public final class DefaultRuntimeScheduler implements RuntimeScheduler {

  @Override
  public Result<RuntimeExecutionPlan, SemanticError> build(SemanticGraph graph) {
    return build(graph, ExecutionProfile.DAG);
  }

  public Result<RuntimeExecutionPlan, SemanticError> build(
      SemanticGraph graph, ExecutionProfile profile) {
    if (profile != ExecutionProfile.DAG) {
      return Result.err(
          new SemanticError(
              ErrorCode.UNSUPPORTED_PROFILE,
              0L,
              "MVP runtime supports DAG profile only, got " + profile));
    }
    Result<List<Long>, SemanticError> order = ExecutionOrdering.topologicalOrder(graph);
    if (!order.isOk()) {
      return Result.err(order.error());
    }
    return Result.ok(new RuntimeExecutionPlan(graph, profile, order.value()));
  }
}
