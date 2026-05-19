package io.sedna.runtime.trace;

import io.sedna.core.ExecutionToken;

/** Single deterministic runtime step (no timestamps). */
public record ExecutionTraceEvent(
    long sequenceNumber, long nodeId, ExecutionToken token, TraceEventKind kind) {

  public ExecutionTraceEvent(long sequenceNumber, long nodeId, ExecutionToken token) {
    this(sequenceNumber, nodeId, token, TraceEventKind.EXECUTE);
  }
}
