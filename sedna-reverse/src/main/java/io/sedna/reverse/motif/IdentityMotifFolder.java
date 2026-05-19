package io.sedna.reverse.motif;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.MotifFolder;
import io.sedna.dna.PassThroughMotifCodec;

/** No-op motif folder for MVP reverse pipeline. */
public final class IdentityMotifFolder implements MotifFolder {

  public static final IdentityMotifFolder INSTANCE = new IdentityMotifFolder();

  private final MotifFolder delegate = PassThroughMotifCodec.INSTANCE;

  private IdentityMotifFolder() {}

  @Override
  public Result<SemanticGraph, SemanticError> fold(SemanticGraph graph) {
    return delegate.fold(graph);
  }
}
