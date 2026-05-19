package io.sedna.reverse.stage;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.reverse.cms.CmsSemanticRules;
import io.sedna.reverse.model.StructuralGraph;

/** Step 3 — structural graph to semantic graph. */
public final class SemanticExtractionStep {

  public Result<SemanticGraph, SemanticError> extract(StructuralGraph structural) {
    if (CmsSemanticRules.isCmsReference(structural)) {
      return Result.ok(CmsSemanticRules.toSemanticGraph(structural));
    }
    return Result.err(
        SemanticError.global(
            ErrorCode.NOT_IMPLEMENTED, "Semantic extraction supported for cms-reference profile only"));
  }
}
