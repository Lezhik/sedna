package io.sedna.validation;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic semantic graph diff (Phase 14 `sedna diff`). */
public final class SemanticGraphDiffer {

  public List<SemanticGraphDiffEntry> diff(SemanticGraph before, SemanticGraph after) {
    SemanticGraph left = CanonicalOrdering.canonicalize(before);
    SemanticGraph right = CanonicalOrdering.canonicalize(after);

    TreeMap<Long, GenomeNode> beforeNodes = index(left);
    TreeMap<Long, GenomeNode> afterNodes = index(right);
    List<SemanticGraphDiffEntry> entries = new ArrayList<>();

    for (var entry : beforeNodes.entrySet()) {
      if (!afterNodes.containsKey(entry.getKey())) {
        entries.add(new SemanticGraphDiffEntry(entry.getKey(), "NODE_REMOVED", entry.getValue().kind().name()));
      }
    }
    for (var entry : afterNodes.entrySet()) {
      if (!beforeNodes.containsKey(entry.getKey())) {
        entries.add(new SemanticGraphDiffEntry(entry.getKey(), "NODE_ADDED", entry.getValue().kind().name()));
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
        entries.add(
            new SemanticGraphDiffEntry(
                nodeId,
                "CORE_CHANGED",
                previous.core().classRef().termPath() + "->" + current.core().classRef().termPath()));
      }
      if (!contractSet(previous).equals(contractSet(current))) {
        entries.add(new SemanticGraphDiffEntry(nodeId, "CONTRACT_CHANGED", contractSignature(current)));
      }
      if (!constraintSet(previous).equals(constraintSet(current))) {
        entries.add(new SemanticGraphDiffEntry(nodeId, "CONSTRAINT_CHANGED", constraintSignature(current)));
      }
      if (previous.kind() != current.kind()) {
        entries.add(
            new SemanticGraphDiffEntry(
                nodeId, "KIND_CHANGED", previous.kind().name() + "->" + current.kind().name()));
      }
    }

    if (!left.vocabularyVersion().equals(right.vocabularyVersion())) {
      entries.add(
          new SemanticGraphDiffEntry(
              0L,
              "REGISTRY_VERSION_CHANGED",
              left.vocabularyVersion().canonical() + "->" + right.vocabularyVersion().canonical()));
    }

    Set<String> beforeLinks = linkKeys(left);
    Set<String> afterLinks = linkKeys(right);
    for (String added : afterLinks) {
      if (!beforeLinks.contains(added)) {
        entries.add(new SemanticGraphDiffEntry(0L, "LINK_ADDED", added));
      }
    }
    for (String removed : beforeLinks) {
      if (!afterLinks.contains(removed)) {
        entries.add(new SemanticGraphDiffEntry(0L, "LINK_REMOVED", removed));
      }
    }

    entries.sort(
        Comparator.comparing(SemanticGraphDiffEntry::kind)
            .thenComparingLong(SemanticGraphDiffEntry::nodeId)
            .thenComparing(SemanticGraphDiffEntry::payload));
    return List.copyOf(entries);
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
    return node.contracts().stream().map(SemanticGraphDiffer::contractKey).sorted().reduce((a, b) -> a + ";" + b).orElse("");
  }

  private static String contractKey(Contract contract) {
    return contract.protocol().name()
        + '|'
        + contract.ioSchema().format()
        + ':'
        + contract.ioSchema().payload()
        + '|'
        + contract.provides().stream().map(c -> c.name() + '@' + c.versionConstraint()).sorted().reduce((a, b) -> a + "," + b).orElse("")
        + '|'
        + contract.requires().stream().map(c -> c.name() + '@' + c.versionConstraint()).sorted().reduce((a, b) -> a + "," + b).orElse("");
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
