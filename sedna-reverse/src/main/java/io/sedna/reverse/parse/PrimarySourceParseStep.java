package io.sedna.reverse.parse;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedProject;
import java.nio.file.Path;

/** Spoon primary, JavaParser fallback (deterministic per-project choice). */
public final class PrimarySourceParseStep implements SourceParseStep {

  private final SourceParseStep primary;
  private final SourceParseStep fallback;

  public PrimarySourceParseStep(SourceParseStep primary, SourceParseStep fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  public static PrimarySourceParseStep spoonWithJavaParserFallback() {
    return new PrimarySourceParseStep(new SpoonSourceParseStep(), new JavaSourceParseStep());
  }

  @Override
  public Result<ParsedProject, SemanticError> parse(Path projectRoot) {
    Result<ParsedProject, SemanticError> spoon = primary.parse(projectRoot);
    if (spoon.isOk()) {
      return spoon;
    }
    return fallback.parse(projectRoot);
  }
}
