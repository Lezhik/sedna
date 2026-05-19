package io.sedna.runtime;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.persistence.CheckpointStore;
import io.sedna.runtime.execution.DagRuntimeExecutor;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.ExecutionTraceEvent;

/** Orchestrates DAG execution and checkpoint persistence. */
public final class RuntimeEngine {

  private final DefaultRuntimeScheduler scheduler;
  private final DagRuntimeExecutor executor;
  private final DnaEncoder encoder;
  private final CheckpointStore checkpointStore;

  public RuntimeEngine(
      DefaultRuntimeScheduler scheduler,
      DagRuntimeExecutor executor,
      DnaEncoder encoder,
      CheckpointStore checkpointStore) {
    this.scheduler = scheduler;
    this.executor = executor;
    this.encoder = encoder;
    this.checkpointStore = checkpointStore;
  }

  public Result<ExecutionTrace, SemanticError> run(SemanticGraph graph) {
    SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
    Result<RuntimeExecutionPlan, SemanticError> plan = scheduler.build(canonical);
    if (!plan.isOk()) {
      return Result.err(plan.error());
    }

    Result<ExecutionTrace, SemanticError> trace = executor.execute(plan.value());
    if (!trace.isOk()) {
      return trace;
    }

    byte[] snapshot = encoder.encode(canonical).value();
    for (ExecutionTraceEvent event : trace.value().events()) {
      Result<?, SemanticError> stored = checkpointStore.append(snapshot, event.token());
      if (!stored.isOk()) {
        return Result.err(stored.error());
      }
    }
    return trace;
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
}
