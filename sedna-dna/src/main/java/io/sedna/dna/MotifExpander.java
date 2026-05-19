package io.sedna.dna;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Motif expansion API — implementation deferred to Phase 3. */
public interface MotifExpander {

  Result<SemanticGraph, SemanticError> expand(SemanticGraph graph);
}
