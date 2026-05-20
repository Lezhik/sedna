package io.sedna.training;

import io.sedna.training.model.MutationDatasetEntry;
import io.sedna.training.model.SemanticSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds mutation training rows from per-commit graph snapshots (Git history). */
public final class MutationDatasetFromHistory {

  private final MutationDatasetGenerator generator = new MutationDatasetGenerator();

  /** Creates a history-based mutation dataset builder. */
  public MutationDatasetFromHistory() {}

  /**
   * Generates mutation rows from per-commit snapshots.
   *
   * @param snapshots ordered semantic snapshots
   * @return mutation entries labeled with commit hash prefix
   */
  public List<MutationDatasetEntry> generate(List<SemanticSnapshot> snapshots) {
    List<MutationDatasetEntry> entries = new ArrayList<>();
    for (SemanticSnapshot snapshot : snapshots) {
      for (MutationDatasetEntry base : generator.generate(snapshot.graph())) {
        entries.add(
            new MutationDatasetEntry(
                base.targetNodeId(),
                base.operation(),
                snapshot.commitHash() + ":" + base.label()));
      }
    }
    entries.sort(
        Comparator.comparing(MutationDatasetEntry::label)
            .thenComparingLong(MutationDatasetEntry::targetNodeId)
            .thenComparing(entry -> entry.operation().name()));
    return List.copyOf(entries);
  }
}
