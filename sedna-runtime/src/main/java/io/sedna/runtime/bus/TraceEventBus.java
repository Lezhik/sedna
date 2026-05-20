package io.sedna.runtime.bus;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import java.util.List;

/** Kafka-compatible trace event bus (Phase 14 prototype). */
public interface TraceEventBus {

  Result<Boolean, SemanticError> publish(ExecutionTraceEvent event);

  Result<List<ExecutionTraceEvent>, SemanticError> drainOrdered();
}
