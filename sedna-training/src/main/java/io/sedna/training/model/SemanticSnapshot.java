package io.sedna.training.model;

import io.sedna.core.SemanticGraph;

/**
 * Graph snapshot bound to a Git commit (or {@code WORKTREE} when no VCS).
 *
 * @param commitHash Git commit hash or {@code WORKTREE}
 * @param graph semantic graph at this snapshot
 * @param dnaFingerprint SHA-256 of encoded DNA bytes
 */
public record SemanticSnapshot(String commitHash, SemanticGraph graph, String dnaFingerprint) {

  /** Validates required string fields. */
  public SemanticSnapshot {
    if (commitHash == null || commitHash.isBlank()) {
      throw new IllegalArgumentException("commitHash required");
    }
    if (dnaFingerprint == null || dnaFingerprint.isBlank()) {
      throw new IllegalArgumentException("dnaFingerprint required");
    }
  }
}
