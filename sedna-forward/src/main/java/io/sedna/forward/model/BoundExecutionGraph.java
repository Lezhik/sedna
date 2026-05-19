package io.sedna.forward.model;

import io.sedna.core.SemanticGraph;
import java.util.List;

/** Graph with materialized contract edges. */
public record BoundExecutionGraph(SemanticGraph graph, List<MaterializedEdge> materializedEdges) {
  public BoundExecutionGraph {
    materializedEdges = List.copyOf(materializedEdges);
  }
}
