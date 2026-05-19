package io.sedna.dna;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Decodes SEDNA-BIN-v1 TLV bytes to semantic graphs. */
public interface DnaDecoder {

  Result<SemanticGraph, SemanticError> decode(byte[] dna);
}
