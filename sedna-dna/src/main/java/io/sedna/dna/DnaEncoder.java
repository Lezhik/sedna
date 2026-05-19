package io.sedna.dna;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Encodes canonical semantic graphs to SEDNA-BIN-v1 TLV bytes. */
public interface DnaEncoder {

  Result<byte[], SemanticError> encode(SemanticGraph graph);
}
