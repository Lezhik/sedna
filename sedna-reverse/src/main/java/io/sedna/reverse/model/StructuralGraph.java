package io.sedna.reverse.model;

import java.util.List;

/**
 * Class-level dependency graph (Step 2 output).
 *
 * @param project parsed project model
 * @param edges structural dependency edges
 */
public record StructuralGraph(ParsedProject project, List<StructuralEdge> edges) {

  /** Defensive copy of edges. */
  public StructuralGraph {
    edges = List.copyOf(edges);
  }
}
