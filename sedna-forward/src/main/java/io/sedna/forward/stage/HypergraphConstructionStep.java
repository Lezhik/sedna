package io.sedna.forward.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.forward.model.SemanticHypergraph;

public final class HypergraphConstructionStep {

  public Result<SemanticHypergraph, SemanticError> build(SemanticGraph graph) {
    return Result.ok(new SemanticHypergraph(graph));
  }
}
