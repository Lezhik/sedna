package io.sedna.training.model;

import io.sedna.core.SemanticGraph;

/** Graph snapshot bound to a Git commit (or {@code WORKTREE} when no VCS). */
public record SemanticSnapshot(String commitHash, SemanticGraph graph, String dnaFingerprint) {
  public SemanticSnapshot {
    if (commitHash == null || commitHash.isBlank()) {
      throw new IllegalArgumentException("commitHash required");
    }
    if (dnaFingerprint == null || dnaFingerprint.isBlank()) {
      throw new IllegalArgumentException("dnaFingerprint required");
    }
  }
}
