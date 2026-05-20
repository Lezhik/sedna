package io.sedna.training;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.training.model.SemanticDelta;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Extracts atomic semantic deltas between consecutive graph snapshots. */
public final class SemanticDeltaExtractor {

  /** Creates a semantic delta extractor. */
  public SemanticDeltaExtractor() {}

  /**
   * Extracts atomic deltas between two graph snapshots.
   *
   * @param commitHash Git commit hash for the transition
   * @param before previous snapshot graph
   * @param after current snapshot graph
   * @return sorted semantic deltas
   */
  public List<SemanticDelta> extract(String commitHash, SemanticGraph before, SemanticGraph after) {
    SemanticGraph left = CanonicalOrdering.canonicalize(before);
    SemanticGraph right = CanonicalOrdering.canonicalize(after);

    TreeMap<Long, GenomeNode> beforeNodes = index(left);
    TreeMap<Long, GenomeNode> afterNodes = index(right);
    List<SemanticDelta> deltas = new ArrayList<>();

    for (var entry : beforeNodes.entrySet()) {
      long nodeId = entry.getKey();
      if (!afterNodes.containsKey(nodeId)) {
        deltas.add(new SemanticDelta(commitHash, nodeId, "NODE_REMOVED", entry.getValue().kind().name()));
      }
    }
    for (var entry : afterNodes.entrySet()) {
      long nodeId = entry.getKey();
      if (!beforeNodes.containsKey(nodeId)) {
        deltas.add(new SemanticDelta(commitHash, nodeId, "NODE_ADDED", entry.getValue().kind().name()));
      }
    }
    for (var entry : beforeNodes.entrySet()) {
      long nodeId = entry.getKey();
      GenomeNode previous = entry.getValue();
      GenomeNode current = afterNodes.get(nodeId);
      if (current == null) {
        continue;
      }
      if (!previous.core().equals(current.core())) {
        deltas.add(
            new SemanticDelta(
                commitHash,
                nodeId,
                "CORE_CHANGED",
                previous.core().classRef().termPath() + "->" + current.core().classRef().termPath()));
      }
      if (!contractSet(previous).equals(contractSet(current))) {
        deltas.add(new SemanticDelta(commitHash, nodeId, "CONTRACT_CHANGED", contractSignature(current)));
      }
      if (!constraintSet(previous).equals(constraintSet(current))) {
        deltas.add(new SemanticDelta(commitHash, nodeId, "CONSTRAINT_CHANGED", constraintSignature(current)));
      }
    }

    Set<String> beforeLinks = linkKeys(left);
    Set<String> afterLinks = linkKeys(right);
    if (!beforeLinks.equals(afterLinks)) {
      for (String added : afterLinks) {
        if (!beforeLinks.contains(added)) {
          deltas.add(new SemanticDelta(commitHash, 0L, "LINK_ADDED", added));
        }
      }
      for (String removed : beforeLinks) {
        if (!afterLinks.contains(removed)) {
          deltas.add(new SemanticDelta(commitHash, 0L, "LINK_REMOVED", removed));
        }
      }
    }

    deltas.sort(
        Comparator.comparing(SemanticDelta::commitHash)
            .thenComparingLong(SemanticDelta::nodeId)
            .thenComparing(SemanticDelta::deltaKind)
            .thenComparing(SemanticDelta::payload));
    return List.copyOf(deltas);
  }

  private static TreeMap<Long, GenomeNode> index(SemanticGraph graph) {
    TreeMap<Long, GenomeNode> map = new TreeMap<>();
    for (GenomeNode node : graph.nodes()) {
      map.put(node.nodeId(), node);
    }
    return map;
  }

  private static Set<String> contractSet(GenomeNode node) {
    Set<String> keys = new TreeSet<>();
    for (Contract contract : node.contracts()) {
      keys.add(contractKey(contract));
    }
    return keys;
  }

  private static String contractSignature(GenomeNode node) {
    return node.contracts().stream().map(SemanticDeltaExtractor::contractKey).sorted().reduce((a, b) -> a + ";" + b).orElse("");
  }

  private static String contractKey(Contract contract) {
    StringBuilder builder = new StringBuilder();
    builder.append(contract.protocol().name()).append('|');
    builder
        .append(contract.ioSchema().format())
        .append(':')
        .append(contract.ioSchema().payload())
        .append('|');
    contract
        .provides()
        .forEach(cap -> builder.append(cap.name()).append('@').append(cap.versionConstraint()).append(','));
    contract
        .requires()
        .forEach(cap -> builder.append('>').append(cap.name()).append('@').append(cap.versionConstraint()).append(','));
    return builder.toString();
  }

  private static Set<String> constraintSet(GenomeNode node) {
    Set<String> codes = new TreeSet<>();
    node.constraints().forEach(constraint -> codes.add(constraint.code()));
    return codes;
  }

  private static String constraintSignature(GenomeNode node) {
    return node.constraints().stream().map(c -> c.code()).sorted().reduce((a, b) -> a + "," + b).orElse("");
  }

  private static Set<String> linkKeys(SemanticGraph graph) {
    Set<String> keys = new TreeSet<>();
    for (SemanticLink link : graph.links()) {
      keys.add(link.sourceNodeId() + ">" + link.targetNodeId() + ">" + link.type().name());
    }
    return keys;
  }
}
