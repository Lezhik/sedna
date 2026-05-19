package io.sedna.runtime.execution;

import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.compensation.CompensationHandler;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Project Reactor DAG executor with canonical sequential ordering. */
public final class DagRuntimeExecutor {

  private final CompensationHandler compensationHandler;

  public DagRuntimeExecutor(CompensationHandler compensationHandler) {
    this.compensationHandler = compensationHandler;
  }

  public Result<ExecutionTrace, SemanticError> execute(RuntimeExecutionPlan plan) {
    SemanticGraph graph = plan.graph();
    AtomicLong sequence = new AtomicLong(0L);

    List<ExecutionTraceEvent> events =
        Flux.fromIterable(plan.orderedNodeIds())
            .concatMap(
                nodeId ->
                    Mono.fromCallable(
                        () -> {
                          GenomeNode node =
                              graph.nodes().stream()
                                  .filter(n -> n.nodeId() == nodeId)
                                  .findFirst()
                                  .orElseThrow();
                          long seq = sequence.incrementAndGet();
                          return new ExecutionTraceEvent(
                              seq, nodeId, DeterministicTokenFactory.token(node, seq));
                        }))
            .collectList()
            .block();

    if (events == null || events.isEmpty()) {
      return Result.ok(new ExecutionTrace(List.of()));
    }

    compensationHandler.compensate(events.getLast().token());
    return Result.ok(new ExecutionTrace(events));
  }
}
