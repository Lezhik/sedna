package io.sedna.validation;

import io.sedna.core.Constraint;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.dna.SednaFoldV1;
import java.util.ArrayList;
import java.util.List;

/** Validates folded MOTIF nodes and surfaces PARTIAL_MATCH flags. */
public final class MotifValidationEngine implements ValidationEngine {

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    List<String> flags = new ArrayList<>();
    for (GenomeNode node : graph.nodes()) {
      if (node.kind() != NodeKind.MOTIF) {
        continue;
      }
      boolean hasPayload = false;
      for (Constraint constraint : node.constraints()) {
        if (constraint.code().startsWith(SednaFoldV1.FOLD_PAYLOAD_PREFIX)) {
          hasPayload = true;
          try {
            SednaFoldMotifCodec.decodeMotifPayload(node);
          } catch (IllegalArgumentException ex) {
            return Result.err(
                new SemanticError(
                    ErrorCode.VALIDATION_FAILED, node.nodeId(), ex.getMessage()));
          }
        }
        if (constraint.code().equals(SednaFoldV1.PARTIAL_MATCH_FLAG)) {
          flags.add("PARTIAL_MATCH:nodeId=" + node.nodeId());
        }
      }
      if (!hasPayload) {
        return Result.err(
            new SemanticError(
                ErrorCode.VALIDATION_FAILED,
                node.nodeId(),
                "MOTIF node missing SEDNA_FOLD_V1 payload"));
      }
    }
    if (flags.isEmpty()) {
      return Result.ok(ValidationReport.success());
    }
    return Result.ok(ValidationReport.successWithFlags(flags));
  }
}
