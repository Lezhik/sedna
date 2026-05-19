package io.sedna.forward.model;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.SemanticGraph;
import java.util.List;

/** Ordered DAG execution plan for codegen. */
public record ExecutionPlan(
    SemanticGraph graph, ExecutionProfile profile, List<Long> orderedNodeIds) {
  public ExecutionPlan {
    orderedNodeIds = List.copyOf(orderedNodeIds);
  }
}
