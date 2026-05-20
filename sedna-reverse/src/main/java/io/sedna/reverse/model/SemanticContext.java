package io.sedna.reverse.model;

/** Execution boundary context (Step 6). */
public enum SemanticContext {
  /** Single-class or method-local scope. */
  LOCAL,
  /** Package or module boundary. */
  MODULE,
  /** Domain aggregate boundary. */
  DOMAIN,
  /** Excluded from shared execution context. */
  ISOLATED
}
