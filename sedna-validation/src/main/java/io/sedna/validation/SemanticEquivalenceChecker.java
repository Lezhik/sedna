package io.sedna.validation;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Contract;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.HashSet;
import java.util.Set;

/** Phase 3 semantic equivalence rules (AGENTS.md §12.2). */
public final class SemanticEquivalenceChecker {

  private SemanticEquivalenceChecker() {}

  public static Result<Boolean, SemanticError> checkEquivalent(SemanticGraph expected, SemanticGraph actual) {
    SemanticGraph left = CanonicalOrdering.canonicalize(expected);
    SemanticGraph right = CanonicalOrdering.canonicalize(actual);

    if (left.nodes().size() != right.nodes().size()) {
      return mismatch("Node count differs: " + left.nodes().size() + " vs " + right.nodes().size());
    }

    Set<Long> leftIds = nodeIds(left);
    Set<Long> rightIds = nodeIds(right);
    if (!leftIds.equals(rightIds)) {
      return mismatch("NodeID set differs");
    }

    for (long nodeId : leftIds) {
      GenomeNode leftNode = findNode(left, nodeId);
      GenomeNode rightNode = findNode(right, nodeId);
      if (leftNode.kind() != rightNode.kind()) {
        return mismatch("Node kind differs for nodeId " + nodeId);
      }
      if (!leftNode.core().equals(rightNode.core())) {
        return mismatch("SemanticCore differs for nodeId " + nodeId);
      }
      if (!contractSet(leftNode).equals(contractSet(rightNode))) {
        return mismatch("Contract set differs for nodeId " + nodeId);
      }
      if (!constraintSet(leftNode).equals(constraintSet(rightNode))) {
        return mismatch("Constraint set differs for nodeId " + nodeId);
      }
    }

    Set<String> leftLinks = linkKeys(left);
    Set<String> rightLinks = linkKeys(right);
    if (!leftLinks.equals(rightLinks)) {
      return mismatch("Dependency topology differs");
    }

    if (!left.vocabularyVersion().equals(right.vocabularyVersion())) {
      return mismatch("Registry version differs");
    }

    return Result.ok(Boolean.TRUE);
  }

  private static Set<Long> nodeIds(SemanticGraph graph) {
    Set<Long> ids = new HashSet<>();
    for (GenomeNode node : graph.nodes()) {
      ids.add(node.nodeId());
    }
    return ids;
  }

  private static GenomeNode findNode(SemanticGraph graph, long nodeId) {
    return graph.nodes().stream()
        .filter(node -> node.nodeId() == nodeId)
        .findFirst()
        .orElseThrow();
  }

  private static Set<String> contractSet(GenomeNode node) {
    Set<String> keys = new HashSet<>();
    for (Contract contract : node.contracts()) {
      keys.add(contractKey(contract));
    }
    return keys;
  }

  private static String contractKey(Contract contract) {
    return contract.provides().stream().map(c -> c.canonical()).sorted().toList()
        + "|"
        + contract.requires().stream().map(c -> c.canonical()).sorted().toList()
        + "|"
        + contract.protocol().name()
        + "|"
        + contract.ioSchema().format()
        + "|"
        + contract.ioSchema().payload();
  }

  private static Set<String> constraintSet(GenomeNode node) {
    return new HashSet<>(node.constraints().stream().map(c -> c.code()).toList());
  }

  private static Set<String> linkKeys(SemanticGraph graph) {
    Set<String> keys = new HashSet<>();
    for (SemanticLink link : graph.links()) {
      keys.add(link.sourceNodeId() + ">" + link.targetNodeId() + ">" + link.type().name());
    }
    return keys;
  }

  private static Result<Boolean, SemanticError> mismatch(String message) {
    return Result.err(SemanticError.global(ErrorCode.VALIDATION_FAILED, message));
  }
}
