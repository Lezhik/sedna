package io.sedna.mutation;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.ErrorCode;
import io.sedna.core.Mutation;
import io.sedna.core.MutationResult;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.MotifExpander;
import io.sedna.dna.MotifFolder;
import io.sedna.validation.CodegenProbe;
import io.sedna.validation.MutationSafetyEngine;
import io.sedna.validation.SemanticEquivalenceChecker;
import io.sedna.validation.ValidationEngine;

/** Apply → validate → commit/rollback mutation transaction. */
public final class DefaultMutationEngine implements MutationEngine {

  private final MutationApplicator applicator;
  private final MutationSafetyEngine mutationSafety;
  private final ValidationEngine validationEngine;
  private final CodegenProbe codegenProbe;

  public DefaultMutationEngine(
      MotifFolder motifFolder,
      MotifExpander motifExpander,
      MutationSafetyEngine mutationSafety,
      ValidationEngine validationEngine,
      CodegenProbe codegenProbe) {
    this.applicator = new MutationApplicator(motifFolder, motifExpander);
    this.mutationSafety = mutationSafety;
    this.validationEngine = validationEngine;
    this.codegenProbe = codegenProbe;
  }

  @Override
  public Result<MutationResult, SemanticError> apply(SemanticGraph graph, Mutation mutation) {
    SemanticGraph baseline = CanonicalOrdering.canonicalize(graph);

    Result<SemanticGraph, SemanticError> applied = applicator.apply(baseline, mutation);
    if (!applied.isOk()) {
      return rollback(baseline, applied.error());
    }

    SemanticGraph candidate = CanonicalOrdering.canonicalize(applied.value());

    var scope = mutationSafety.verifySubtreeScope(baseline, candidate, mutation.targetNodeId());
    if (!scope.isOk()) {
      return rollback(baseline, scope.error());
    }

    var validated = validationEngine.validate(candidate);
    if (!validated.isOk()) {
      return rollback(baseline, validated.error());
    }
    if (!validated.value().valid()) {
      return rollback(
          baseline,
          validated.value().errors().isEmpty()
              ? SemanticError.global(ErrorCode.VALIDATION_FAILED, "Validation failed")
              : validated.value().errors().getFirst());
    }

    var probe = codegenProbe.probe(candidate);
    if (!probe.isOk()) {
      return rollback(baseline, probe.error());
    }

    return Result.ok(new MutationResult(candidate, false));
  }

  public Result<Boolean, SemanticError> verifyEquivalenceAfterMutation(
      SemanticGraph before, SemanticGraph after) {
    return SemanticEquivalenceChecker.checkEquivalent(before, after);
  }

  private static Result<MutationResult, SemanticError> rollback(
      SemanticGraph baseline, SemanticError cause) {
    return Result.ok(new MutationResult(baseline, true));
  }
}
