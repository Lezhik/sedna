package io.sedna.forward.stage;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.forward.codegen.CmsCodeGenerator;
import io.sedna.forward.codegen.SpringBootCodeGenerator;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.model.ExecutionPlan;
import io.sedna.forward.model.GeneratedProject;
import io.sedna.forward.util.SpringBootNaming;

public final class CodeGenerationStep {

  private final CmsCodeGenerator cmsGenerator;
  private final SpringBootCodeGenerator springBootGenerator;

  public CodeGenerationStep(LlmClient llmClient) {
    this.cmsGenerator = new CmsCodeGenerator(llmClient);
    this.springBootGenerator = new SpringBootCodeGenerator(llmClient);
  }

  public Result<GeneratedProject, SemanticError> generate(ExecutionPlan plan) {
    if (SpringBootNaming.hasSourcePackage(plan.graph())) {
      return springBootGenerator.generate(plan);
    }
    return cmsGenerator.generate(plan);
  }
}
