package io.sedna.runtime.compensation;

import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;

/** SUPERVISOR compensation hook (MVP: no-op placeholder per FR-rt.05). */
public interface CompensationHandler {

  Result<Boolean, SemanticError> compensate(ExecutionToken failedToken);

  static CompensationHandler noOp() {
    return token -> Result.ok(Boolean.TRUE);
  }
}
