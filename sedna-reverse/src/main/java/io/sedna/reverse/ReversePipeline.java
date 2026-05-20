package io.sedna.reverse;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.MotifFolder;
import io.sedna.registry.SemanticRegistry;
import io.sedna.reverse.model.StructuralGraph;
import io.sedna.reverse.motif.GraphSignatureMotifFolder;
import io.sedna.reverse.parse.BytecodeDependencyAugmenter;
import io.sedna.reverse.parse.PrimarySourceParseStep;
import io.sedna.reverse.parse.SourceParseStep;
import io.sedna.reverse.stage.ContextReconstructionStep;
import io.sedna.reverse.stage.ContractReconstructionStep;
import io.sedna.reverse.stage.GenomeSerializationStep;
import io.sedna.reverse.stage.MotifFoldingStep;
import io.sedna.reverse.stage.SemanticExtractionStep;
import io.sedna.reverse.structural.StructuralGraphStep;
import io.sedna.reverse.unknown.DisabledUnknownLabelProvider;
import io.sedna.reverse.unknown.UnknownNodeEnrichmentStep;
import io.sedna.validation.CompositeValidationEngine;
import io.sedna.validation.ValidationEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Spring Boot project → SEDNA DNA reverse pipeline. */
public final class ReversePipeline {

  private final SourceParseStep sourceParseStep = PrimarySourceParseStep.spoonWithJavaParserFallback();
  private final BytecodeDependencyAugmenter bytecodeDependencyAugmenter =
      new BytecodeDependencyAugmenter();
  private final StructuralGraphStep structuralGraphStep = new StructuralGraphStep();
  private final SemanticExtractionStep semanticExtractionStep = new SemanticExtractionStep();
  private final ContractReconstructionStep contractReconstructionStep = new ContractReconstructionStep();
  private final MotifFoldingStep motifFoldingStep;
  private final ContextReconstructionStep contextReconstructionStep = new ContextReconstructionStep();
  private final GenomeSerializationStep genomeSerializationStep;
  private final UnknownNodeEnrichmentStep unknownNodeEnrichmentStep = new UnknownNodeEnrichmentStep();
  private final ValidationEngine validationEngine;

  /**
   * Creates a reverse pipeline with validation and motif folding.
   *
   * @param encoder DNA encoder for final serialization
   * @param registry semantic registry for validation
   * @param motifFolder motif folding strategy
   */
  public ReversePipeline(DnaEncoder encoder, SemanticRegistry registry, MotifFolder motifFolder) {
    this.motifFoldingStep = new MotifFoldingStep(motifFolder);
    this.genomeSerializationStep = new GenomeSerializationStep(encoder);
    this.validationEngine = CompositeValidationEngine.standard(registry);
  }

  /**
   * Returns a pipeline with graph-signature motif folding.
   *
   * @param encoder DNA encoder for final serialization
   * @param registry semantic registry for validation
   * @return configured reverse pipeline
   */
  public static ReversePipeline standard(DnaEncoder encoder, SemanticRegistry registry) {
    return new ReversePipeline(encoder, registry, GraphSignatureMotifFolder.INSTANCE);
  }

  /**
   * Runs the full reverse pipeline and returns the semantic graph.
   *
   * @param projectRoot Gradle project root
   * @return validated semantic graph or structured error
   */
  public Result<SemanticGraph, SemanticError> reverseGraph(Path projectRoot) {
    var parsed = sourceParseStep.parse(projectRoot);
    if (!parsed.isOk()) {
      return Result.err(parsed.error());
    }
    var augmented = bytecodeDependencyAugmenter.augment(parsed.value());
    if (!augmented.isOk()) {
      return Result.err(augmented.error());
    }

    var structural = structuralGraphStep.build(augmented.value());
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

    var enriched =
        unknownNodeEnrichmentStep.enrich(
            structural.value(),
            contracts.value(),
            DisabledUnknownLabelProvider.INSTANCE);
    if (!enriched.isOk()) {
      return Result.err(enriched.error());
    }

    var folded = motifFoldingStep.fold(enriched.value());
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

    return Result.ok(graph);
  }

  /**
   * Builds the structural graph without semantic extraction (profile detection, trajectories).
   *
   * @param projectRoot Gradle project root
   * @return structural graph or structured error
   */
  public Result<StructuralGraph, SemanticError> buildStructuralGraph(Path projectRoot) {
    var parsed = sourceParseStep.parse(projectRoot);
    if (!parsed.isOk()) {
      return Result.err(parsed.error());
    }
    var augmented = bytecodeDependencyAugmenter.augment(parsed.value());
    if (!augmented.isOk()) {
      return Result.err(augmented.error());
    }
    return structuralGraphStep.build(augmented.value());
  }

  /**
   * Runs the reverse pipeline and returns encoded DNA bytes.
   *
   * @param projectRoot Gradle project root
   * @return SEDNA-BIN-v1 bytes or structured error
   */
  public Result<byte[], SemanticError> reverse(Path projectRoot) {
    var graph = reverseGraph(projectRoot);
    if (!graph.isOk()) {
      return Result.err(graph.error());
    }
    return genomeSerializationStep.serialize(graph.value());
  }

  /**
   * Runs the reverse pipeline and writes DNA to a file.
   *
   * @param projectRoot Gradle project root
   * @param outputDnaFile destination {@code .sdna} path
   * @return written file path or structured error
   */
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
