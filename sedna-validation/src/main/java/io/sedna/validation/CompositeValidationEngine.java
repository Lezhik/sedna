package io.sedna.validation;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import java.util.ArrayList;
import java.util.List;

/** Runs validation engines in order; returns first failure. */
public final class CompositeValidationEngine implements ValidationEngine {

  private final List<ValidationEngine> engines;

  public CompositeValidationEngine(List<ValidationEngine> engines) {
    this.engines = List.copyOf(engines);
  }

  public static CompositeValidationEngine standard(io.sedna.registry.SemanticRegistry registry) {
    List<ValidationEngine> chain = new ArrayList<>();
    chain.add(new GraphValidationEngine());
    chain.add(new VocabularyVersionValidationEngine(registry));
    chain.add(new VocabularyValidationEngine(registry));
    chain.add(new MotifValidationEngine());
    return new CompositeValidationEngine(chain);
  }

  @Override
  public Result<ValidationReport, SemanticError> validate(SemanticGraph graph) {
    List<String> flags = new ArrayList<>();
    for (ValidationEngine engine : engines) {
      Result<ValidationReport, SemanticError> result = engine.validate(graph);
      if (!result.isOk()) {
        return result;
      }
      if (!result.value().valid()) {
        return result;
      }
      flags.addAll(result.value().flags());
    }
    if (flags.isEmpty()) {
      return Result.ok(ValidationReport.success());
    }
    return Result.ok(ValidationReport.successWithFlags(flags));
  }
}
