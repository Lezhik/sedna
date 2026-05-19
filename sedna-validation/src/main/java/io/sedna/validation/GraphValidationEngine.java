package io.sedna.validation;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.HashSet;
import java.util.Set;

/** Phase 1 graph topology and NodeID validation. */
public final class GraphValidationEngine implements ValidationEngine {

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    Set<Long> nodeIds = new HashSet<>();
    for (GenomeNode node : graph.nodes()) {
      if (!nodeIds.add(node.nodeId())) {
        return Result.err(
            new SemanticError(
                ErrorCode.VALIDATION_FAILED, node.nodeId(), "Duplicate nodeId in graph"));
      }
    }
    for (SemanticLink link : graph.links()) {
      if (!nodeIds.contains(link.sourceNodeId())) {
        return Result.err(
            new SemanticError(
                ErrorCode.VALIDATION_FAILED,
                link.sourceNodeId(),
                "Link source nodeId not found"));
      }
      if (!nodeIds.contains(link.targetNodeId())) {
        return Result.err(
            new SemanticError(
                ErrorCode.VALIDATION_FAILED,
                link.targetNodeId(),
                "Link target nodeId not found"));
      }
    }
    return Result.ok(ValidationReport.success());
  }
}
