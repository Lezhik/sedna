package io.sedna.dna;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Identity motif fold/expand for MVP mutation and reverse pipelines. */
public final class PassThroughMotifCodec implements MotifFolder, MotifExpander {

  public static final PassThroughMotifCodec INSTANCE = new PassThroughMotifCodec();

  private PassThroughMotifCodec() {}

  @Override
  public Result<SemanticGraph, SemanticError> fold(SemanticGraph graph) {
    return Result.ok(graph);
  }

  @Override
  public Result<SemanticGraph, SemanticError> expand(SemanticGraph graph) {
    return Result.ok(graph);
  }
}
