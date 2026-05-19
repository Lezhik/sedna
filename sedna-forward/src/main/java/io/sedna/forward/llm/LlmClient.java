package io.sedna.forward.llm;

import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;

/** Method-body generation boundary (LLM or deterministic fallback). */
public interface LlmClient {

  Result<String, SemanticError> generateMethodBody(GenomeNode node, String methodSignature);
}
