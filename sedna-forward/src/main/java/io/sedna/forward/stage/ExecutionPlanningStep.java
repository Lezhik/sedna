package io.sedna.forward.stage;

import io.sedna.core.ExecutionOrdering;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.forward.model.BoundExecutionGraph;
import io.sedna.forward.model.ExecutionPlan;
import java.util.List;

public final class ExecutionPlanningStep {

  public Result<ExecutionPlan, SemanticError> plan(BoundExecutionGraph bound) {
    SemanticGraph graph = bound.graph();
    Result<List<Long>, SemanticError> order = ExecutionOrdering.topologicalOrder(graph);
    if (!order.isOk()) {
      return Result.err(order.error());
    }
    return Result.ok(new ExecutionPlan(graph, ExecutionProfile.DAG, order.value()));
  }
}
