package io.sedna.validation;

import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.registry.SemanticRegistry;

/** Resolves all vocabulary references in a graph against the semantic registry. */
public final class VocabularyValidationEngine implements ValidationEngine {

  private final SemanticRegistry registry;

  public VocabularyValidationEngine(SemanticRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    for (GenomeNode node : graph.nodes()) {
      for (VocabRef ref : allVocabRefs(node)) {
        if (ref.termPath().startsWith("UNKNOWN.INSTANCE.")) {
          continue;
        }
        var resolved = registry.resolve(ref);
        if (!resolved.isOk()) {
          return Result.err(resolved.error());
        }
      }
    }
    return Result.ok(ValidationReport.success());
  }

  private static Iterable<VocabRef> allVocabRefs(GenomeNode node) {
    return () ->
        java.util.stream.Stream.concat(
                java.util.stream.Stream.of(
                    node.core().classRef(),
                    node.core().targetRef(),
                    node.core().operationRef()),
                node.core().modifiers().stream())
            .iterator();
  }
}
