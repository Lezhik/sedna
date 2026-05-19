package io.sedna.forward.llm;

import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;

/** Deterministic fallback: empty method body skeleton. */
public final class DisabledLlmClient implements LlmClient {

  public static final DisabledLlmClient INSTANCE = new DisabledLlmClient();

  private DisabledLlmClient() {}

  @Override
  public Result<String, SemanticError> generateMethodBody(GenomeNode node, String methodSignature) {
    return Result.ok("        // LLM disabled — implement " + methodSignature);
  }
}
