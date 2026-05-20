package io.sedna.persistence;

import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.util.List;

/** Append-only checkpoint persistence. */
public interface CheckpointStore {

  Result<CheckpointRecord, SemanticError> append(byte[] graphSnapshotRef, ExecutionToken token);

  default Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef, ExecutionToken token, String fsmState, int completedNodes) {
    return append(graphSnapshotRef, token, fsmState, completedNodes, "DAG", 0L);
  }

  default Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef,
      ExecutionToken token,
      String fsmState,
      int completedNodes,
      String executionProfile) {
    return append(graphSnapshotRef, token, fsmState, completedNodes, executionProfile, 0L);
  }

  Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef,
      ExecutionToken token,
      String fsmState,
      int completedNodes,
      String executionProfile,
      long injectFailureNodeId);

  Result<List<CheckpointRecord>, SemanticError> listOrdered();

  Result<CheckpointRecord, SemanticError> findBySequence(long sequenceNumber);
}
