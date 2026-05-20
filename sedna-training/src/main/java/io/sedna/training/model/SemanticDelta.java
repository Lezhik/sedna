package io.sedna.training.model;

/**
 * Atomic semantic transition between two snapshots (one node or one contract).
 *
 * @param commitHash Git commit hash associated with the transition
 * @param nodeId affected genome node id ({@code 0} for link-level deltas)
 * @param deltaKind delta category (e.g. {@code NODE_ADDED})
 * @param payload human-readable delta payload
 */
public record SemanticDelta(
    String commitHash, long nodeId, String deltaKind, String payload) {

  /** Validates required fields and normalizes payload. */
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
