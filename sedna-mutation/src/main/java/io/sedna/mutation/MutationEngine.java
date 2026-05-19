package io.sedna.mutation;

import io.sedna.core.Mutation;
import io.sedna.core.MutationResult;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Subtree-scoped semantic graph mutations with transactional validation. */
public interface MutationEngine {

  Result<MutationResult, SemanticError> apply(SemanticGraph graph, Mutation mutation);
}
