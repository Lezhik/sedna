package io.sedna.runtime;

import io.sedna.core.ExecutionOrdering;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.profile.ProfileTransitionValidator;
import java.util.List;

/** Canonical runtime scheduler for DAG, STATEFUL, and SUPERVISOR profiles. */
public final class DefaultRuntimeScheduler implements RuntimeScheduler {

  @Override
  public Result<RuntimeExecutionPlan, SemanticError> build(SemanticGraph graph) {
    return build(graph, ExecutionProfile.DAG);
  }

  public Result<RuntimeExecutionPlan, SemanticError> build(
      SemanticGraph graph, ExecutionProfile profile) {
    var profileCheck = ProfileTransitionValidator.validate(graph, profile);
    if (!profileCheck.isOk()) {
      return Result.err(profileCheck.error());
    }
    Result<List<Long>, SemanticError> order = ExecutionOrdering.topologicalOrder(graph);
    if (!order.isOk()) {
      return Result.err(order.error());
    }
    return Result.ok(new RuntimeExecutionPlan(graph, profile, order.value()));
  }
}
