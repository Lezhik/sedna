package io.sedna.reverse.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Step 4 — contracts are embedded during CMS semantic extraction (MVP). */
public final class ContractReconstructionStep {

  /** Creates a contract reconstruction step. */
  public ContractReconstructionStep() {}

  /**
   * Returns the input graph unchanged (contracts already attached in Step 3).
   *
   * @param graph semantic graph with embedded contracts
   * @return the same graph wrapped in {@link Result}
   */
  public Result<SemanticGraph, SemanticError> reconstruct(SemanticGraph graph) {
    return Result.ok(graph);
  }
}
