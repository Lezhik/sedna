package io.sedna.dna;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Motif folding API — implementation deferred to Phase 3. */
public interface MotifFolder {

  Result<SemanticGraph, SemanticError> fold(SemanticGraph graph);
}
