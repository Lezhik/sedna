package io.sedna.persistence;

import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory append-only checkpoint log for tests and local runs. */
public final class InMemoryCheckpointStore implements CheckpointStore {

  private final List<CheckpointRecord> records = new ArrayList<>();
  private final AtomicLong idSequence = new AtomicLong(0);
  private final AtomicLong sequenceNumber = new AtomicLong(0);

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef, ExecutionToken token) {
    return append(graphSnapshotRef, token, "", 0);
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef, ExecutionToken token, String fsmState, int completedNodes) {
    long id = idSequence.incrementAndGet();
    long seq = sequenceNumber.incrementAndGet();
    CheckpointRecord record =
        new CheckpointRecord(id, token, graphSnapshotRef, seq, fsmState, completedNodes);
    records.add(record);
    return Result.ok(record);
  }

  @Override
  public synchronized Result<List<CheckpointRecord>, SemanticError> listOrdered() {
    return Result.ok(
        records.stream()
            .sorted(Comparator.comparingLong(CheckpointRecord::sequenceNumber))
            .toList());
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> findBySequence(long sequence) {
    for (CheckpointRecord record : records) {
      if (record.sequenceNumber() == sequence) {
        return Result.ok(record);
      }
    }
    return Result.err(
        SemanticError.global(ErrorCode.VALIDATION_FAILED, "No checkpoint at sequence " + sequence));
  }
}
