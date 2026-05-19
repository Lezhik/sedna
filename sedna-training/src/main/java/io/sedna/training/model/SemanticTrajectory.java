package io.sedna.training.model;

import java.util.List;

/** Deterministic semantic evolution trajectory for one project folder. */
public record SemanticTrajectory(
    String projectPath,
    List<String> commitOrder,
    List<SemanticSnapshot> snapshots,
    List<SemanticDelta> deltas) {
  public SemanticTrajectory {
    commitOrder = List.copyOf(commitOrder);
    snapshots = List.copyOf(snapshots);
    deltas = List.copyOf(deltas);
  }
}
