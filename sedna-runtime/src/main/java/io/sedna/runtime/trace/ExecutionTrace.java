package io.sedna.runtime.trace;

import java.util.List;

/** Ordered execution trace for replay comparison. */
public record ExecutionTrace(List<ExecutionTraceEvent> events) {
  public ExecutionTrace {
    events = List.copyOf(events);
  }
}
