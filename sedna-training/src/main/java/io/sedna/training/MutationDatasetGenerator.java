package io.sedna.training;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.GenomeNode;
import io.sedna.core.MutationType;
import io.sedna.core.SemanticGraph;
import io.sedna.training.model.MutationDatasetEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Generates deterministic mutation training candidates (not applied). */
public final class MutationDatasetGenerator {

  /** Creates a mutation dataset generator. */
  public MutationDatasetGenerator() {}

  /**
   * Generates deterministic mutation candidates for a graph.
   *
   * @param graph semantic graph
   * @return sorted mutation entries (not applied)
   */
  public List<MutationDatasetEntry> generate(SemanticGraph graph) {
    SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
    List<MutationDatasetEntry> entries = new ArrayList<>();
    for (GenomeNode node : canonical.nodes()) {
      switch (node.kind()) {
        case SERVICE -> entries.add(
            new MutationDatasetEntry(node.nodeId(), MutationType.CONSTRAINT_INJECTION, "STATELESS_ONLY"));
        case CONTROLLER -> entries.add(
            new MutationDatasetEntry(node.nodeId(), MutationType.CONSTRAINT_INJECTION, "READ_ONLY"));
        case ENTITY -> entries.add(
            new MutationDatasetEntry(node.nodeId(), MutationType.CONTRACT_UPGRADE, "CAPABILITY_BUMP"));
        case MOTIF -> {
          entries.add(
              new MutationDatasetEntry(node.nodeId(), MutationType.MOTIF_EXPAND, "CRUD_STACK"));
          entries.add(
              new MutationDatasetEntry(node.nodeId(), MutationType.MOTIF_FOLD, "CRUD_STACK"));
        }
        default -> {}
      }
    }
    entries.sort(
        Comparator.comparingLong(MutationDatasetEntry::targetNodeId)
            .thenComparing(entry -> entry.operation().name())
            .thenComparing(MutationDatasetEntry::label));
    return List.copyOf(entries);
  }
}
