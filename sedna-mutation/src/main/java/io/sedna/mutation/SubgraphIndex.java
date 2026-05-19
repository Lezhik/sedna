package io.sedna.mutation;

import io.sedna.core.LinkType;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Dependency subgraph utilities for subtree-scoped mutations. */
public final class SubgraphIndex {

  private SubgraphIndex() {}

  public static Set<Long> subtreeNodeIds(SemanticGraph graph, long rootNodeId) {
    Set<Long> subtree = new TreeSet<>();
    subtree.add(rootNodeId);
    Map<Long, List<Long>> dependencies = dependencyTargets(graph);
    ArrayDeque<Long> queue = new ArrayDeque<>();
    queue.add(rootNodeId);
    while (!queue.isEmpty()) {
      long current = queue.removeFirst();
      for (long target : dependencies.getOrDefault(current, List.of())) {
        if (subtree.add(target)) {
          queue.addLast(target);
        }
      }
    }
    return Set.copyOf(subtree);
  }

  public static List<SemanticLink> localLinks(SemanticGraph graph, Set<Long> nodeIds) {
    return graph.links().stream()
        .filter(link -> nodeIds.contains(link.sourceNodeId()) && nodeIds.contains(link.targetNodeId()))
        .toList();
  }

  private static Map<Long, List<Long>> dependencyTargets(SemanticGraph graph) {
    Map<Long, List<Long>> map = new HashMap<>();
    for (var node : graph.nodes()) {
      map.put(node.nodeId(), new ArrayList<>());
    }
    for (SemanticLink link : graph.links()) {
      if (link.type() == LinkType.DEPENDENCY) {
        map.get(link.sourceNodeId()).add(link.targetNodeId());
      }
    }
    for (var entry : map.entrySet()) {
      entry.setValue(entry.getValue().stream().sorted().distinct().toList());
    }
    return map;
  }
}
