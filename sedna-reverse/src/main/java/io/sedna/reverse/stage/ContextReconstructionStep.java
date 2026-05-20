package io.sedna.reverse.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.SemanticContext;
import io.sedna.reverse.model.StructuralGraph;
import java.util.LinkedHashMap;
import java.util.Map;

/** Step 6 — infer semantic context per class (metadata map; not stored in DNA MVP). */
public final class ContextReconstructionStep {

  /**
   * Semantic graph with per-class context metadata.
   *
   * @param graph validated semantic graph
   * @param contexts context label per qualified class name
   */
  public record ContextualizedGraph(SemanticGraph graph, Map<String, SemanticContext> contexts) {

    /** Defensive copy of context map. */
    public ContextualizedGraph {
      contexts = Map.copyOf(contexts);
    }
  }

  /** Creates a context reconstruction step. */
  public ContextReconstructionStep() {}

  /**
   * Assigns a semantic context to each parsed class.
   *
   * @param structural structural dependency graph
   * @param graph semantic graph from prior stages
   * @return contextualized graph or structured error
   */
  public Result<ContextualizedGraph, SemanticError> reconstruct(
      StructuralGraph structural, SemanticGraph graph) {
    Map<String, SemanticContext> contexts = new LinkedHashMap<>();
    for (ParsedClass parsed : structural.project().classes()) {
      SemanticContext context =
          parsed.packageName().startsWith("io.sedna.cms") ? SemanticContext.MODULE : SemanticContext.LOCAL;
      contexts.put(parsed.qualifiedName(), context);
    }
    return Result.ok(new ContextualizedGraph(graph, contexts));
  }
}
