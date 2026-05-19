package io.sedna.reverse.model;

import java.util.List;

/** Class-level dependency graph (Step 2 output). */
public record StructuralGraph(ParsedProject project, List<StructuralEdge> edges) {
  public StructuralGraph {
    edges = List.copyOf(edges);
  }
}
