package io.sedna.validation;

import io.sedna.core.ErrorCode;
import io.sedna.core.RegistryVersionCompatibility;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.registry.SemanticRegistry;

/** Validates graph {@link io.sedna.core.RegistryVersion} against loaded registry (FR-reg.03). */
public final class VocabularyVersionValidationEngine implements ValidationEngine {

  private final SemanticRegistry registry;

  public VocabularyVersionValidationEngine(SemanticRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    if (!RegistryVersionCompatibility.isCompatible(graph.vocabularyVersion(), registry.version())) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED,
              RegistryVersionCompatibility.incompatibilityReason(
                  graph.vocabularyVersion(), registry.version())));
    }
    return Result.ok(ValidationReport.success());
  }
}
