package io.sedna.reverse;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.MotifFolder;
import io.sedna.registry.SemanticRegistry;
import io.sedna.reverse.git.GitTrajectoryStep;
import io.sedna.reverse.motif.IdentityMotifFolder;
import io.sedna.reverse.parse.JavaSourceParseStep;
import io.sedna.reverse.stage.ContextReconstructionStep;
import io.sedna.reverse.stage.ContractReconstructionStep;
import io.sedna.reverse.stage.GenomeSerializationStep;
import io.sedna.reverse.stage.MotifFoldingStep;
import io.sedna.reverse.stage.SemanticExtractionStep;
import io.sedna.reverse.structural.StructuralGraphStep;
import io.sedna.validation.CompositeValidationEngine;
import io.sedna.validation.ValidationEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Spring Boot project → SEDNA DNA reverse pipeline. */
public final class ReversePipeline {

  private final JavaSourceParseStep javaSourceParseStep = new JavaSourceParseStep();
  private final StructuralGraphStep structuralGraphStep = new StructuralGraphStep();
  private final SemanticExtractionStep semanticExtractionStep = new SemanticExtractionStep();
  private final ContractReconstructionStep contractReconstructionStep = new ContractReconstructionStep();
  private final MotifFoldingStep motifFoldingStep;
  private final ContextReconstructionStep contextReconstructionStep = new ContextReconstructionStep();
  private final GenomeSerializationStep genomeSerializationStep;
  private final GitTrajectoryStep gitTrajectoryStep = new GitTrajectoryStep();
  private final ValidationEngine validationEngine;

  public ReversePipeline(DnaEncoder encoder, SemanticRegistry registry, MotifFolder motifFolder) {
    this.motifFoldingStep = new MotifFoldingStep(motifFolder);
    this.genomeSerializationStep = new GenomeSerializationStep(encoder);
    this.validationEngine = CompositeValidationEngine.standard(registry);
  }

  public static ReversePipeline standard(DnaEncoder encoder, SemanticRegistry registry) {
    return new ReversePipeline(encoder, registry, IdentityMotifFolder.INSTANCE);
  }

  public Result<SemanticGraph, SemanticError> reverseGraph(Path projectRoot) {
    var parsed = javaSourceParseStep.parse(projectRoot);
    if (!parsed.isOk()) {
      return Result.err(parsed.error());
    }

    var structural = structuralGraphStep.build(parsed.value());
    if (!structural.isOk()) {
      return Result.err(structural.error());
    }

    var semantic = semanticExtractionStep.extract(structural.value());
    if (!semantic.isOk()) {
      return Result.err(semantic.error());
    }

    var contracts = contractReconstructionStep.reconstruct(semantic.value());
    if (!contracts.isOk()) {
      return Result.err(contracts.error());
    }

    var folded = motifFoldingStep.fold(contracts.value());
    if (!folded.isOk()) {
      return Result.err(folded.error());
    }

    var contextualized = contextReconstructionStep.reconstruct(structural.value(), folded.value());
    if (!contextualized.isOk()) {
      return Result.err(contextualized.error());
    }

    SemanticGraph graph = CanonicalOrdering.canonicalize(contextualized.value().graph());
    var validated = validationEngine.validate(graph);
    if (!validated.isOk()) {
      return Result.err(validated.error());
    }
    if (!validated.value().valid()) {
      var errors = validated.value().errors();
      return Result.err(
          errors.isEmpty()
              ? SemanticError.global(io.sedna.core.ErrorCode.VALIDATION_FAILED, "Validation failed")
              : errors.getFirst());
    }

    gitTrajectoryStep.extract(projectRoot);
    return Result.ok(graph);
  }

  public Result<byte[], SemanticError> reverse(Path projectRoot) {
    var graph = reverseGraph(projectRoot);
    if (!graph.isOk()) {
      return Result.err(graph.error());
    }
    return genomeSerializationStep.serialize(graph.value());
  }

  public Result<Path, SemanticError> reverseToFile(Path projectRoot, Path outputDnaFile) {
    var dna = reverse(projectRoot);
    if (!dna.isOk()) {
      return Result.err(dna.error());
    }
    try {
      Path parent = outputDnaFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(outputDnaFile, dna.value());
      return Result.ok(outputDnaFile);
    } catch (IOException ex) {
      return Result.err(SemanticError.global(io.sedna.core.ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
