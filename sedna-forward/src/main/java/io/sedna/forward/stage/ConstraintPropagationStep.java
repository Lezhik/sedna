package io.sedna.forward.stage;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.forward.model.BoundExecutionGraph;

public final class ConstraintPropagationStep {

  public Result<BoundExecutionGraph, SemanticError> propagate(BoundExecutionGraph graph) {
    for (GenomeNode node : graph.graph().nodes()) {
      for (var constraint : node.constraints()) {
        if (!isKnownConstraint(constraint.code())) {
          return Result.err(
              new SemanticError(
                  ErrorCode.CONSTRAINT_VIOLATION,
                  node.nodeId(),
                  "Unknown constraint: " + constraint.code()));
        }
      }
    }
    return Result.ok(graph);
  }

  private static boolean isKnownConstraint(String code) {
    return switch (code) {
      case "STATELESS_ONLY", "TRANSACTIONAL", "READ_ONLY" -> true;
      default -> code.startsWith("SOURCE_PACKAGE:");
    };
  }
}
