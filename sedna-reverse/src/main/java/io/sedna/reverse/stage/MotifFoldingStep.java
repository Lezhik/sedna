package io.sedna.reverse.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.MotifFolder;

/** Step 5 — motif folding (SEDNA-FOLD-v1 CRUD_STACK detection). */
public final class MotifFoldingStep {

  private final MotifFolder motifFolder;

  public MotifFoldingStep(MotifFolder motifFolder) {
    this.motifFolder = motifFolder;
  }

  public Result<SemanticGraph, SemanticError> fold(SemanticGraph graph) {
    return motifFolder.fold(graph);
  }
}
