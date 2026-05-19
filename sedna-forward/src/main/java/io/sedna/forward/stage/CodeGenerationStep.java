package io.sedna.forward.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.forward.codegen.CmsCodeGenerator;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.model.ExecutionPlan;
import io.sedna.forward.model.GeneratedProject;

public final class CodeGenerationStep {

  private final CmsCodeGenerator generator;

  public CodeGenerationStep(LlmClient llmClient) {
    this.generator = new CmsCodeGenerator(llmClient);
  }

  public Result<GeneratedProject, SemanticError> generate(ExecutionPlan plan) {
    return generator.generate(plan);
  }
}
