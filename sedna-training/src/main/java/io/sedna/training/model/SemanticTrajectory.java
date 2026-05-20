package io.sedna.training.model;

import java.util.List;

/**
 * Deterministic semantic evolution trajectory for one project folder.
 *
 * @param projectPath absolute project path
 * @param commitOrder Git commit hashes oldest-first
 * @param snapshots semantic snapshots per commit
 * @param deltas semantic deltas between consecutive snapshots
 */
public record SemanticTrajectory(
    String projectPath,
    List<String> commitOrder,
    List<SemanticSnapshot> snapshots,
    List<SemanticDelta> deltas) {

  /** Defensive copy of list components. */
  public SemanticTrajectory {
    commitOrder = List.copyOf(commitOrder);
    snapshots = List.copyOf(snapshots);
    deltas = List.copyOf(deltas);
  }
}
