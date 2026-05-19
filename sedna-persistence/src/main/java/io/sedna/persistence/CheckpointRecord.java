package io.sedna.persistence;

import io.sedna.core.ExecutionToken;
import java.util.Arrays;

/** Append-only checkpoint row (FR-rt.02 / design §2.3). */
public record CheckpointRecord(
    long id, ExecutionToken executionToken, byte[] graphSnapshotRef, long sequenceNumber) {

  public CheckpointRecord {
    graphSnapshotRef = Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
  }

  @Override
  public byte[] graphSnapshotRef() {
    return Arrays.copyOf(graphSnapshotRef, graphSnapshotRef.length);
  }
}
