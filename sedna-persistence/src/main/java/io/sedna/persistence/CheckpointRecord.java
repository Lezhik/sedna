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
    int completedNodes) {

  public CheckpointRecord(
      long id, ExecutionToken executionToken, byte[] graphSnapshotRef, long sequenceNumber) {
    this(id, executionToken, graphSnapshotRef, sequenceNumber, "", 0);
  }

  public CheckpointRecord {
    graphSnapshotRef = Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
    fsmState = fsmState == null ? "" : fsmState;
  }

  @Override
  public byte[] graphSnapshotRef() {
    return Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
  }
}
