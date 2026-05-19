package io.sedna.runtime.trace;

import io.sedna.core.ExecutionToken;

/** Single deterministic runtime step (no timestamps). */
public record ExecutionTraceEvent(long sequenceNumber, long nodeId, ExecutionToken token) {}
