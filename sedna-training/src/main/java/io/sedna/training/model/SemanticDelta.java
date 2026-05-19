package io.sedna.training.model;

/** Atomic semantic transition between two snapshots (one node or one contract). */
public record SemanticDelta(
    String commitHash, long nodeId, String deltaKind, String payload) {
  public SemanticDelta {
    if (commitHash == null || commitHash.isBlank()) {
      throw new IllegalArgumentException("commitHash required");
    }
    if (deltaKind == null || deltaKind.isBlank()) {
      throw new IllegalArgumentException("deltaKind required");
    }
    payload = payload == null ? "" : payload;
  }
}
