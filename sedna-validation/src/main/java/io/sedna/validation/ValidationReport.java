package io.sedna.validation;

import io.sedna.core.SemanticError;
import java.util.List;

/** Aggregated validation outcome. */
public record ValidationReport(boolean valid, List<SemanticError> errors) {
  public ValidationReport {
    errors = List.copyOf(errors);
  }

  public static ValidationReport success() {
    return new ValidationReport(true, List.of());
  }

  public static ValidationReport failure(List<SemanticError> errors) {
    return new ValidationReport(false, errors);
  }
}
