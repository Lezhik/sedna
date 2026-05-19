package io.sedna.forward.stage;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.forward.model.BoundExecutionGraph;
import io.sedna.forward.model.ExecutionPlan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExecutionPlanningStep {

  public Result<ExecutionPlan, SemanticError> plan(BoundExecutionGraph bound) {
    SemanticGraph graph = bound.graph();
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
        var cycle = visit(node.nodeId(), dependencies, visiting, visited, ordered);
        if (cycle.isPresent()) {
          return Result.err(
              new SemanticError(
                  ErrorCode.VALIDATION_FAILED, cycle.get(), "Cycle detected in dependency graph"));
        }
      }
    }

    return Result.ok(new ExecutionPlan(graph, ExecutionProfile.DAG, List.copyOf(ordered)));
  }

  private static java.util.Optional<Long> visit(
      long nodeId,
      Map<Long, Set<Long>> dependencies,
      Set<Long> visiting,
      Set<Long> visited,
      List<Long> ordered) {
    if (visited.contains(nodeId)) {
      return java.util.Optional.empty();
    }
    if (!visiting.add(nodeId)) {
      return java.util.Optional.of(nodeId);
    }
    Set<Long> deps = dependencies.getOrDefault(nodeId, Set.of());
    List<Long> sortedDeps = deps.stream().sorted().toList();
    for (long dep : sortedDeps) {
      var cycle = visit(dep, dependencies, visiting, visited, ordered);
      if (cycle.isPresent()) {
        return cycle;
      }
    }
    visiting.remove(nodeId);
    visited.add(nodeId);
    ordered.add(nodeId);
    return java.util.Optional.empty();
  }
}
