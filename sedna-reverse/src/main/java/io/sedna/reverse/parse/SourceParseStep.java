package io.sedna.reverse.parse;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedProject;
import java.nio.file.Path;

/** Step 1 — parse Java sources into a structural project model. */
public interface SourceParseStep {

  /**
   * Parses Java sources under {@code src/main/java}.
   *
   * @param projectRoot Gradle project root
   * @return parsed project or structured error
   */
  Result<ParsedProject, SemanticError> parse(Path projectRoot);
}
