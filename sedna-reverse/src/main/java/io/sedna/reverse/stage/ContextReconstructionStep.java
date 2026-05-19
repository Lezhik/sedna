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

  public record ContextualizedGraph(SemanticGraph graph, Map<String, SemanticContext> contexts) {
    public ContextualizedGraph {
      contexts = Map.copyOf(contexts);
    }
  }

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
