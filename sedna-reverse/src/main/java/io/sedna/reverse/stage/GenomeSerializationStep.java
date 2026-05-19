package io.sedna.reverse.stage;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;

/** Step 7 — encode semantic graph to SEDNA-BIN-v1 TLV bytes. */
public final class GenomeSerializationStep {

  private final DnaEncoder encoder;

  public GenomeSerializationStep(DnaEncoder encoder) {
    this.encoder = encoder;
  }

  public Result<byte[], SemanticError> serialize(SemanticGraph graph) {
    return encoder.encode(CanonicalOrdering.canonicalize(graph));
  }
}
