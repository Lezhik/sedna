package io.sedna.runtime;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.persistence.CheckpointStore;
import io.sedna.runtime.execution.ProfileRuntimeExecutor;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import io.sedna.runtime.trace.TraceEventKind;
import java.util.ArrayList;
import java.util.List;

/** Orchestrates profile-aware execution and checkpoint persistence. */
public final class RuntimeEngine {

  private final DefaultRuntimeScheduler scheduler;
  private final ProfileRuntimeExecutor executor;
  private final DnaEncoder encoder;
  private final CheckpointStore checkpointStore;

  public RuntimeEngine(
      DefaultRuntimeScheduler scheduler,
      ProfileRuntimeExecutor executor,
      DnaEncoder encoder,
      CheckpointStore checkpointStore) {
    this.scheduler = scheduler;
    this.executor = executor;
    this.encoder = encoder;
    this.checkpointStore = checkpointStore;
  }

  public Result<ExecutionTrace, SemanticError> run(SemanticGraph graph) {
    return run(graph, ExecutionProfile.DAG, RuntimeExecutionOptions.DEFAULT);
  }

  public Result<ExecutionTrace, SemanticError> run(
      SemanticGraph graph, ExecutionProfile profile, RuntimeExecutionOptions options) {
    SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
    Result<RuntimeExecutionPlan, SemanticError> plan = scheduler.build(canonical, profile);
    if (!plan.isOk()) {
      return Result.err(plan.error());
    }

    Result<ExecutionTrace, SemanticError> trace = executor.execute(plan.value(), options);
    if (!trace.isOk()) {
      return trace;
    }

    return persistCheckpoints(canonical, plan.value(), trace.value());
  }

  public Result<ExecutionTrace, SemanticError> restoreAndContinue(long fromSequence) {
    var checkpoint = checkpointStore.findBySequence(fromSequence);
    if (!checkpoint.isOk()) {
      return Result.err(checkpoint.error());
    }
    var decoded =
        io.sedna.dna.DnaServices.decoder().decode(checkpoint.value().graphSnapshotRef());
    if (!decoded.isOk()) {
      return Result.err(decoded.error());
    }
    return run(decoded.value());
  }

  public Result<ExecutionTrace, SemanticError> resumeStateful(long fromSequence) {
    var checkpoint = checkpointStore.findBySequence(fromSequence);
    if (!checkpoint.isOk()) {
      return Result.err(checkpoint.error());
    }
    var decoded =
        io.sedna.dna.DnaServices.decoder().decode(checkpoint.value().graphSnapshotRef());
    if (!decoded.isOk()) {
      return Result.err(decoded.error());
    }

    var plan = scheduler.build(decoded.value(), ExecutionProfile.STATEFUL);
    if (!plan.isOk()) {
      return Result.err(plan.error());
    }

    var resumed =
        executor.execute(
            plan.value(),
            RuntimeExecutionOptions.resume(
                checkpoint.value().completedNodes(), checkpoint.value().fsmState()));
    if (!resumed.isOk()) {
      return resumed;
    }

    var prefix = eventsThroughSequence(fromSequence);
    if (!prefix.isOk()) {
      return Result.err(prefix.error());
    }

    List<ExecutionTraceEvent> merged = new ArrayList<>(prefix.value());
    long base = fromSequence;
    for (ExecutionTraceEvent event : resumed.value().events()) {
      merged.add(
          new ExecutionTraceEvent(base + event.sequenceNumber(), event.nodeId(), event.token(), event.kind()));
    }
    return Result.ok(new ExecutionTrace(merged));
  }

  private Result<List<ExecutionTraceEvent>, SemanticError> eventsThroughSequence(long throughSequence) {
    var checkpoints = checkpointStore.listOrdered();
    if (!checkpoints.isOk()) {
      return Result.err(checkpoints.error());
    }
    List<ExecutionTraceEvent> events = new ArrayList<>();
    for (var record : checkpoints.value()) {
      if (record.sequenceNumber() > throughSequence) {
        break;
      }
      events.add(
          new ExecutionTraceEvent(
              record.sequenceNumber(), 0L, record.executionToken(), TraceEventKind.EXECUTE));
    }
    return Result.ok(events);
  }

  private Result<ExecutionTrace, SemanticError> persistCheckpoints(
      SemanticGraph canonical, RuntimeExecutionPlan plan, ExecutionTrace trace) {
    byte[] snapshot = encoder.encode(canonical).value();
    int completedNodes = 0;
    String fsmState = executor.fsmStateAfter(trace, plan);

    for (ExecutionTraceEvent event : trace.events()) {
      if (event.kind() == TraceEventKind.EXECUTE) {
        completedNodes++;
      }
      Result<?, SemanticError> stored =
          checkpointStore.append(snapshot, event.token(), fsmState, completedNodes);
      if (!stored.isOk()) {
        return Result.err(stored.error());
      }
    }
    return Result.ok(trace);
  }
}
