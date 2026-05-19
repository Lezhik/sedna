package io.sedna.validation;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Phase 0 stub — returns NOT_IMPLEMENTED until Phase 1 validators ship. */
public final class StubValidationEngine implements ValidationEngine {

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    return Result.err(
        SemanticError.global(
            ErrorCode.NOT_IMPLEMENTED, "Validation engine not implemented (Phase 1)"));
  }
}
