package io.sedna.validation;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Validates semantic graphs before pipeline commit points. */
public interface ValidationEngine {

  Result<ValidationReport, SemanticError> validate(SemanticGraph graph);
}
