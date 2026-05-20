package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.training.model.TrainingDataset;
import io.sedna.training.model.TrainingDatasetArtifacts;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes manifest, checksum, and reproducibility report for a training dataset. */
public final class TrainingDatasetWriter {

  public Result<TrainingDatasetArtifacts, SemanticError> write(
      TrainingDataset dataset, Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
      SemanticEmbeddingIndex index = SemanticEmbeddingIndex.fromDataset(dataset);
      String manifestBody = TrainingManifestBuilder.build(dataset);
      String manifestChecksum = TrainingManifestHasher.sha256(manifestBody);
      String reportBody = TrainingReproducibilityReport.build(dataset, manifestBody, index);

      Path manifest = outputDirectory.resolve("dataset.manifest");
      Path checksum = outputDirectory.resolve("dataset.manifest.sha256");
      Path report = outputDirectory.resolve("reproducibility.report");

      Files.writeString(manifest, manifestBody, StandardCharsets.UTF_8);
      Files.writeString(checksum, manifestChecksum + System.lineSeparator(), StandardCharsets.UTF_8);
      Files.writeString(report, reportBody, StandardCharsets.UTF_8);

      return Result.ok(new TrainingDatasetArtifacts(manifest, checksum, report));
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
