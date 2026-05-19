package io.sedna.validation;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Post-mutation codegen feasibility probe (MVP: DNA round-trip). */
public interface CodegenProbe {

  Result<Boolean, SemanticError> probe(SemanticGraph graph);
}
