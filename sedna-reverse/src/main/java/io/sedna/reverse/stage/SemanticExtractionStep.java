package io.sedna.reverse.stage;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.reverse.cms.CmsSemanticRules;
import io.sedna.reverse.model.StructuralGraph;
import io.sedna.reverse.spring.SpringBootSemanticRules;

/** Step 3 — structural graph to semantic graph. */
public final class SemanticExtractionStep {

  /** Creates a semantic extraction step. */
  public SemanticExtractionStep() {}

  /**
   * Selects a reverse profile and builds the semantic graph.
   *
   * @param structural class-level dependency graph
   * @return semantic graph or structured error
   */
  public Result<SemanticGraph, SemanticError> extract(StructuralGraph structural) {
    if (CmsSemanticRules.isCmsReference(structural)) {
      return Result.ok(CmsSemanticRules.toSemanticGraph(structural));
    }
    if (SpringBootSemanticRules.isSpringBootMonolith(structural)) {
      try {
        return Result.ok(SpringBootSemanticRules.toSemanticGraph(structural));
      } catch (RuntimeException ex) {
        return Result.err(
            SemanticError.global(ErrorCode.VALIDATION_FAILED, ex.getMessage()));
      }
    }
    return Result.err(
        SemanticError.global(
            ErrorCode.NOT_IMPLEMENTED,
            "Semantic extraction supports cms-reference and Spring Boot monolith profiles only"));
  }
}
