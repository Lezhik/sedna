package io.sedna.reverse.motif;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.MotifFolder;
import io.sedna.dna.SednaFoldMotifCodec;

/** Graph-signature motif folding via SEDNA-FOLD-v1 (CRUD_STACK detection). */
public final class GraphSignatureMotifFolder implements MotifFolder {

  public static final GraphSignatureMotifFolder INSTANCE = new GraphSignatureMotifFolder();

  private final MotifFolder delegate = SednaFoldMotifCodec.INSTANCE;

  private GraphSignatureMotifFolder() {}

  @Override
  public Result<SemanticGraph, SemanticError> fold(SemanticGraph graph) {
    return delegate.fold(graph);
  }
}
