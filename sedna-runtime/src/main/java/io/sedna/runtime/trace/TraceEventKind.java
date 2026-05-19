package io.sedna.runtime.trace;

/** Canonical runtime trace event kinds (included in replay hash). */
public enum TraceEventKind {
  EXECUTE,
  STATE_TRANSITION,
  COMPENSATE
}
