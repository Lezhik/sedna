package io.sedna.persistence;

import io.sedna.core.ExecutionToken;
import java.util.Arrays;

/** Append-only checkpoint row (FR-rt.02 / design §2.3). */
public record CheckpointRecord(
    long id,
    ExecutionToken executionToken,
    byte[] graphSnapshotRef,
    long sequenceNumber,
    String fsmState,
    int completedNodes,
    String executionProfile,
    long injectFailureNodeId) {

  public CheckpointRecord(
      long id, ExecutionToken executionToken, byte[] graphSnapshotRef, long sequenceNumber) {
    this(id, executionToken, graphSnapshotRef, sequenceNumber, "", 0, "DAG", 0L);
  }

  public CheckpointRecord(
      long id,
      ExecutionToken executionToken,
      byte[] graphSnapshotRef,
      long sequenceNumber,
      String fsmState,
      int completedNodes,
      String executionProfile) {
    this(
        id,
        executionToken,
        graphSnapshotRef,
        sequenceNumber,
        fsmState,
        completedNodes,
        executionProfile,
        0L);
  }

  public CheckpointRecord {
    graphSnapshotRef = Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
    fsmState = fsmState == null ? "" : fsmState;
    executionProfile = executionProfile == null || executionProfile.isBlank() ? "DAG" : executionProfile;
  }

  @Override
  public byte[] graphSnapshotRef() {
    return Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
  }
}
