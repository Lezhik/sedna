package io.sedna.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Canonical DAG topological ordering (topology + NodeID tie-break). */
public final class ExecutionOrdering {

  private ExecutionOrdering() {}

  public static Result<List<Long>, SemanticError> topologicalOrder(SemanticGraph graph) {
    Map<Long, Set<Long>> dependencies = new HashMap<>();
    for (GenomeNode node : graph.nodes()) {
      dependencies.put(node.nodeId(), new HashSet<>());
    }
    for (SemanticLink link : graph.links()) {
      if (link.type() == LinkType.DEPENDENCY) {
        dependencies.get(link.sourceNodeId()).add(link.targetNodeId());
      }
    }

    List<Long> ordered = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    Set<Long> visiting = new HashSet<>();
    for (GenomeNode node : CanonicalOrdering.sortNodes(graph.nodes())) {
      if (!visited.contains(node.nodeId())) {
        Optional<Long> cycle = visit(node.nodeId(), dependencies, visiting, visited, ordered);
        if (cycle.isPresent()) {
          return Result.err(
              new SemanticError(
                  ErrorCode.VALIDATION_FAILED, cycle.get(), "Cycle detected in dependency graph"));
        }
      }
    }
    return Result.ok(List.copyOf(ordered));
  }

  private static Optional<Long> visit(
      long nodeId,
      Map<Long, Set<Long>> dependencies,
      Set<Long> visiting,
      Set<Long> visited,
      List<Long> ordered) {
    if (visited.contains(nodeId)) {
      return Optional.empty();
    }
    if (!visiting.add(nodeId)) {
      return Optional.of(nodeId);
    }
    Set<Long> deps = dependencies.getOrDefault(nodeId, Set.of());
    List<Long> sortedDeps = deps.stream().sorted().toList();
    for (long dep : sortedDeps) {
      Optional<Long> cycle = visit(dep, dependencies, visiting, visited, ordered);
      if (cycle.isPresent()) {
        return cycle;
      }
    }
    visiting.remove(nodeId);
    visited.add(nodeId);
    ordered.add(nodeId);
    return Optional.empty();
  }
}
