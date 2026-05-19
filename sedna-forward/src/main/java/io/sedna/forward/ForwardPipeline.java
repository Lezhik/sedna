package io.sedna.forward;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaDecoder;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.model.GeneratedProject;
import io.sedna.forward.stage.CodeGenerationStep;
import io.sedna.forward.stage.ConstraintPropagationStep;
import io.sedna.forward.stage.ContractResolutionStep;
import io.sedna.forward.stage.DnaParseStep;
import io.sedna.forward.stage.ExecutionPlanningStep;
import io.sedna.forward.stage.HypergraphConstructionStep;
import io.sedna.forward.stage.RegistryResolutionStep;
import io.sedna.registry.SemanticRegistry;
import io.sedna.validation.CompositeValidationEngine;
import io.sedna.validation.ValidationEngine;
import java.io.IOException;
import java.nio.file.Path;

/** DNA → Spring Boot forward pipeline (deterministic through codegen structure). */
public final class ForwardPipeline {

  private final DnaParseStep dnaParseStep;
  private final RegistryResolutionStep registryResolutionStep;
  private final HypergraphConstructionStep hypergraphConstructionStep;
  private final ContractResolutionStep contractResolutionStep;
  private final ConstraintPropagationStep constraintPropagationStep;
  private final ExecutionPlanningStep executionPlanningStep;
  private final CodeGenerationStep codeGenerationStep;
  private final ValidationEngine validationEngine;

  public ForwardPipeline(
      DnaDecoder decoder,
      SemanticRegistry registry,
      LlmClient llmClient,
      ValidationEngine validationEngine) {
    this.dnaParseStep = new DnaParseStep(decoder);
    this.registryResolutionStep = new RegistryResolutionStep(registry);
    this.hypergraphConstructionStep = new HypergraphConstructionStep();
    this.contractResolutionStep = new ContractResolutionStep();
    this.constraintPropagationStep = new ConstraintPropagationStep();
    this.executionPlanningStep = new ExecutionPlanningStep();
    this.codeGenerationStep = new CodeGenerationStep(llmClient);
    this.validationEngine = validationEngine;
  }

  public static ForwardPipeline standard(DnaDecoder decoder, SemanticRegistry registry, LlmClient llmClient) {
    return new ForwardPipeline(
        decoder, registry, llmClient, CompositeValidationEngine.standard(registry));
  }

  public Result<GeneratedProject, SemanticError> run(byte[] dna) {
    Result<SemanticGraph, SemanticError> parsed = dnaParseStep.parse(dna);
    if (!parsed.isOk()) {
      return Result.err(parsed.error());
    }
    return run(parsed.value());
  }

  public Result<GeneratedProject, SemanticError> run(SemanticGraph graph) {
    var validated = validationEngine.validate(graph);
    if (!validated.isOk()) {
      return Result.err(validated.error());
    }
    if (!validated.value().valid()) {
      var errors = validated.value().errors();
      if (errors.isEmpty()) {
        return Result.err(
            SemanticError.global(io.sedna.core.ErrorCode.VALIDATION_FAILED, "Validation failed"));
      }
      return Result.err(errors.getFirst());
    }

    var resolved = registryResolutionStep.resolve(graph);
    if (!resolved.isOk()) {
      return Result.err(resolved.error());
    }

    var hypergraph = hypergraphConstructionStep.build(resolved.value());
    if (!hypergraph.isOk()) {
      return Result.err(hypergraph.error());
    }

    var bound = contractResolutionStep.resolve(hypergraph.value());
    if (!bound.isOk()) {
      return Result.err(bound.error());
    }

    var constrained = constraintPropagationStep.propagate(bound.value());
    if (!constrained.isOk()) {
      return Result.err(constrained.error());
    }

    var plan = executionPlanningStep.plan(constrained.value());
    if (!plan.isOk()) {
      return Result.err(plan.error());
    }

    return codeGenerationStep.generate(plan.value());
  }

  public Result<GeneratedProject, SemanticError> runToDirectory(byte[] dna, Path outputDirectory) {
    Result<GeneratedProject, SemanticError> generated = run(dna);
    if (!generated.isOk()) {
      return generated;
    }
    try {
      ProjectWriter.write(generated.value(), outputDirectory);
      return generated;
    } catch (IOException ex) {
      return Result.err(SemanticError.global(io.sedna.core.ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
