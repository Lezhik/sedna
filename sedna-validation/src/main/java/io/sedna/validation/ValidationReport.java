package io.sedna.validation;

import io.sedna.core.SemanticError;
import java.util.List;

/** Aggregated validation outcome. */
public record ValidationReport(boolean valid, List<SemanticError> errors, List<String> flags) {
  public ValidationReport {
    errors = List.copyOf(errors);
    flags = List.copyOf(flags);
  }

  public static ValidationReport success() {
    return new ValidationReport(true, List.of(), List.of());
  }

  public static ValidationReport successWithFlags(List<String> flags) {
    return new ValidationReport(true, List.of(), flags);
  }

  public static ValidationReport failure(List<SemanticError> errors) {
    return new ValidationReport(false, errors, List.of());
  }
}
