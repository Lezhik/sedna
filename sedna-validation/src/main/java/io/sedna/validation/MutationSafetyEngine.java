package io.sedna.validation;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Ensures mutations do not rewrite nodes outside the declared subtree. */
public final class MutationSafetyEngine {

  public Result<Boolean, SemanticError> verifySubtreeScope(
      SemanticGraph before, SemanticGraph after, long subtreeRoot) {
    Set<Long> allowed = subtreeNodeIds(before, subtreeRoot);
    Set<Long> beforeIds = new HashSet<>();
    for (GenomeNode node : before.nodes()) {
      beforeIds.add(node.nodeId());
    }
    Set<Long> allowedAfter = new HashSet<>(allowed);
    for (GenomeNode node : after.nodes()) {
      long nodeId = node.nodeId();
      if (!beforeIds.contains(nodeId)
          && isDependencyChildOfSubtree(after, allowed, nodeId)) {
        allowedAfter.add(nodeId);
      }
    }

    for (GenomeNode node : before.nodes()) {
      if (allowed.contains(node.nodeId())) {
        continue;
      }
      GenomeNode afterNode = findNode(after, node.nodeId());
      if (afterNode == null || !afterNode.equals(node)) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "Cross-domain rewrite detected for nodeId " + node.nodeId()));
      }
    }

    for (GenomeNode node : after.nodes()) {
      if (allowedAfter.contains(node.nodeId())) {
        continue;
      }
      GenomeNode beforeNode = findNode(before, node.nodeId());
      if (beforeNode == null || !beforeNode.equals(node)) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "Cross-domain node introduced: " + node.nodeId()));
      }
    }

    Set<String> beforeExternalLinks = externalLinkKeys(before, allowed);
    Set<String> afterExternalLinks = externalLinkKeys(after, allowedAfter);
    if (!beforeExternalLinks.equals(afterExternalLinks)) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "Cross-domain link rewrite detected"));
    }

    return Result.ok(Boolean.TRUE);
  }

  private static Set<Long> subtreeNodeIds(SemanticGraph graph, long rootNodeId) {
    Set<Long> subtree = new HashSet<>();
    subtree.add(rootNodeId);
    Map<Long, Set<Long>> deps = new HashMap<>();
    for (var node : graph.nodes()) {
      deps.put(node.nodeId(), new HashSet<>());
    }
    for (SemanticLink link : graph.links()) {
      deps.get(link.sourceNodeId()).add(link.targetNodeId());
    }
    var queue = new java.util.ArrayDeque<Long>();
    queue.add(rootNodeId);
    while (!queue.isEmpty()) {
      long current = queue.removeFirst();
      for (long target : deps.getOrDefault(current, Set.of())) {
        if (subtree.add(target)) {
          queue.addLast(target);
        }
      }
    }
    return subtree;
  }

  private static Set<String> externalLinkKeys(SemanticGraph graph, Set<Long> allowed) {
    Set<String> keys = new HashSet<>();
    for (SemanticLink link : graph.links()) {
      if (!allowed.contains(link.sourceNodeId()) || !allowed.contains(link.targetNodeId())) {
        keys.add(link.sourceNodeId() + ">" + link.targetNodeId() + ">" + link.type().name());
      }
    }
    return keys;
  }

  private static boolean isDependencyChildOfSubtree(
      SemanticGraph graph, Set<Long> subtree, long nodeId) {
    for (SemanticLink link : graph.links()) {
      if (link.type() == LinkType.DEPENDENCY
          && link.targetNodeId() == nodeId
          && subtree.contains(link.sourceNodeId())) {
        return true;
      }
    }
    return false;
  }

  private static GenomeNode findNode(SemanticGraph graph, long nodeId) {
    return graph.nodes().stream().filter(node -> node.nodeId() == nodeId).findFirst().orElse(null);
  }
}
