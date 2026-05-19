package io.sedna.forward.stage;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.registry.SemanticRegistry;

public final class RegistryResolutionStep {

  private final SemanticRegistry registry;

  public RegistryResolutionStep(SemanticRegistry registry) {
    this.registry = registry;
  }

  public Result<SemanticGraph, SemanticError> resolve(SemanticGraph graph) {
    for (GenomeNode node : graph.nodes()) {
      SemanticCore core = node.core();
      var classResult = registry.resolve(core.classRef());
      if (!classResult.isOk()) {
        return Result.err(classResult.error());
      }
      var targetResult = registry.resolve(core.targetRef());
      if (!targetResult.isOk()) {
        return Result.err(targetResult.error());
      }
      var operationResult = registry.resolve(core.operationRef());
      if (!operationResult.isOk()) {
        return Result.err(operationResult.error());
      }
      for (VocabRef modifier : core.modifiers()) {
        var modifierResult = registry.resolve(modifier);
        if (!modifierResult.isOk()) {
          return Result.err(modifierResult.error());
        }
      }
    }
    if (!graph.vocabularyVersion().equals(registry.version())) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED,
              "Graph vocabularyVersion "
                  + graph.vocabularyVersion().canonical()
                  + " does not match registry "
                  + registry.version().canonical()));
    }
    return Result.ok(graph);
  }
}
