package io.sedna.runtime.profile;

import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Validates profile selection against graph structure (formal spec §4). */
public final class ProfileTransitionValidator {

  private ProfileTransitionValidator() {}

  public static Result<Boolean, SemanticError> validate(SemanticGraph graph, ExecutionProfile profile) {
    if (graph.nodes().isEmpty()) {
      return Result.err(SemanticError.global(ErrorCode.VALIDATION_FAILED, "Graph has no nodes"));
    }
    return switch (profile) {
      case DAG -> Result.ok(Boolean.TRUE);
      case STATEFUL -> {
        if (graph.nodes().isEmpty()) {
          yield Result.err(
              SemanticError.global(ErrorCode.UNSUPPORTED_PROFILE, "STATEFUL requires nodes"));
        }
        yield Result.ok(Boolean.TRUE);
      }
      case SUPERVISOR -> {
        if (graph.nodes().size() < 2) {
          yield Result.err(
              SemanticError.global(
                  ErrorCode.UNSUPPORTED_PROFILE, "SUPERVISOR requires at least two nodes"));
        }
        yield Result.ok(Boolean.TRUE);
      }
    };
  }
}
