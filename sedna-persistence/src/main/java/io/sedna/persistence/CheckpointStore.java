package io.sedna.persistence;

import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.util.List;

/** Append-only checkpoint persistence. */
public interface CheckpointStore {

  Result<CheckpointRecord, SemanticError> append(byte[] graphSnapshotRef, ExecutionToken token);

  Result<List<CheckpointRecord>, SemanticError> listOrdered();

  Result<CheckpointRecord, SemanticError> findBySequence(long sequenceNumber);
}
