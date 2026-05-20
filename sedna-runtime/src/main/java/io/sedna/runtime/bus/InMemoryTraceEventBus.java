package io.sedna.runtime.bus;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** In-memory trace bus for local runs and tests. */
public final class InMemoryTraceEventBus implements TraceEventBus {

  private final List<ExecutionTraceEvent> events = new ArrayList<>();

  @Override
  public synchronized Result<Boolean, SemanticError> publish(ExecutionTraceEvent event) {
    events.add(event);
    return Result.ok(Boolean.TRUE);
  }

  @Override
  public synchronized Result<List<ExecutionTraceEvent>, SemanticError> drainOrdered() {
    List<ExecutionTraceEvent> copy = new ArrayList<>(events);
    copy.sort(
        Comparator.comparingLong(ExecutionTraceEvent::sequenceNumber)
            .thenComparingLong(ExecutionTraceEvent::nodeId));
    return Result.ok(List.copyOf(copy));
  }
}
