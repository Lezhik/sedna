package io.sedna.forward.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaDecoder;

public final class DnaParseStep {

  private final DnaDecoder decoder;

  public DnaParseStep(DnaDecoder decoder) {
    this.decoder = decoder;
  }

  public Result<SemanticGraph, SemanticError> parse(byte[] dna) {
    return decoder.decode(dna);
  }
}
