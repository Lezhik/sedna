package io.sedna.reverse.parse;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedProject;
import java.nio.file.Path;

/** Spoon primary, JavaParser fallback (deterministic per-project choice). */
public final class PrimarySourceParseStep implements SourceParseStep {

  private final SourceParseStep primary;
  private final SourceParseStep fallback;

  /**
   * Creates a primary/fallback parse step chain.
   *
   * @param primary first parser attempt (typically Spoon)
   * @param fallback parser used when primary fails
   */
  public PrimarySourceParseStep(SourceParseStep primary, SourceParseStep fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  /**
   * Returns Spoon primary with JavaParser fallback.
   *
   * @return configured primary parse step
   */
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
