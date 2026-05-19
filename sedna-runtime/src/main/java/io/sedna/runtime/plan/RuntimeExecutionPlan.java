package io.sedna.runtime.plan;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.SemanticGraph;
import java.util.List;

/** Runtime DAG execution plan. */
public record RuntimeExecutionPlan(
    SemanticGraph graph, ExecutionProfile profile, List<Long> orderedNodeIds) {
  public RuntimeExecutionPlan {
    orderedNodeIds = List.copyOf(orderedNodeIds);
  }
}
