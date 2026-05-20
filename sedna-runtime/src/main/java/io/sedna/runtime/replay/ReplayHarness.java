package io.sedna.runtime.replay;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.dna.DnaDecoder;
import io.sedna.persistence.CheckpointRecord;
import io.sedna.persistence.CheckpointStore;
import io.sedna.runtime.DefaultRuntimeScheduler;
import io.sedna.runtime.execution.ProfileRuntimeExecutor;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.TraceHasher;

/** Replay execution from persisted checkpoints. */
public final class ReplayHarness {

  private final DnaDecoder decoder;
  private final DefaultRuntimeScheduler scheduler;
  private final ProfileRuntimeExecutor executor;
  private final CheckpointStore checkpointStore;

  public ReplayHarness(
      DnaDecoder decoder,
      DefaultRuntimeScheduler scheduler,
      ProfileRuntimeExecutor executor,
      CheckpointStore checkpointStore) {
    this.decoder = decoder;
    this.scheduler = scheduler;
    this.executor = executor;
    this.checkpointStore = checkpointStore;
  }

  public Result<ExecutionTrace, SemanticError> replayFromCheckpoint(long sequenceNumber) {
    var checkpoint = checkpointStore.findBySequence(sequenceNumber);
    if (!checkpoint.isOk()) {
      return Result.err(checkpoint.error());
    }
    return replayFromRecord(checkpoint.value());
  }

  public Result<ExecutionTrace, SemanticError> replayFromLastCheckpoint() {
    var checkpoints = checkpointStore.listOrdered();
    if (!checkpoints.isOk()) {
      return Result.err(checkpoints.error());
    }
    if (checkpoints.value().isEmpty()) {
      return Result.err(
          SemanticError.global(io.sedna.core.ErrorCode.VALIDATION_FAILED, "No checkpoints"));
    }
    return replayFromRecord(checkpoints.value().getLast());
  }

  private Result<ExecutionTrace, SemanticError> replayFromRecord(CheckpointRecord checkpoint) {
    var decoded = decoder.decode(checkpoint.graphSnapshotRef());
    if (!decoded.isOk()) {
      return Result.err(decoded.error());
    }

    ExecutionProfile profile = profileFromCheckpoint(checkpoint);
    RuntimeExecutionOptions options = optionsFromCheckpoint(checkpoint);
    Result<RuntimeExecutionPlan, SemanticError> plan = scheduler.build(decoded.value(), profile);
    if (!plan.isOk()) {
      return Result.err(plan.error());
    }

    return executor.execute(plan.value(), options);
  }

  public Result<Boolean, SemanticError> verifyReplayMatches(ExecutionTrace original) {
    Result<ExecutionTrace, SemanticError> replayed = replayFromLastCheckpoint();
    if (!replayed.isOk()) {
      return Result.err(replayed.error());
    }
    String originalHash = TraceHasher.sha256(original);
    String replayHash = TraceHasher.sha256(replayed.value());
    if (!originalHash.equals(replayHash)) {
      return Result.err(
          SemanticError.global(
              io.sedna.core.ErrorCode.VALIDATION_FAILED,
              "Replay trace hash mismatch: expected " + originalHash + " got " + replayHash));
    }
    return Result.ok(Boolean.TRUE);
  }

  private static ExecutionProfile profileFromCheckpoint(CheckpointRecord checkpoint) {
    try {
      return ExecutionProfile.valueOf(checkpoint.executionProfile());
    } catch (IllegalArgumentException ex) {
      return ExecutionProfile.DAG;
    }
  }

  private static RuntimeExecutionOptions optionsFromCheckpoint(CheckpointRecord checkpoint) {
    if (checkpoint.injectFailureNodeId() != 0L) {
      return RuntimeExecutionOptions.injectFailure(checkpoint.injectFailureNodeId());
    }
    return RuntimeExecutionOptions.DEFAULT;
  }
}
