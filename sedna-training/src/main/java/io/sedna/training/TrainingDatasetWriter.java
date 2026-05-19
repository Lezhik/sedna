package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.training.model.TrainingDataset;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a deterministic training dataset manifest. */
public final class TrainingDatasetWriter {

  public Result<Path, SemanticError> write(TrainingDataset dataset, Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
      Path manifest = outputDirectory.resolve("dataset.manifest");
      StringBuilder builder = new StringBuilder();
      builder.append("sedna-training-dataset-v1\n");
      builder.append("fingerprint=").append(dataset.datasetFingerprint()).append('\n');
      builder.append("projects=").append(dataset.projects().size()).append('\n');
      for (var project : dataset.projects()) {
        builder.append("[project]\n");
        builder.append("path=").append(project.projectPath()).append('\n');
        builder.append("commits=").append(String.join(",", project.trajectory().commitOrder())).append('\n');
        builder.append("snapshots=").append(project.trajectory().snapshots().size()).append('\n');
        builder.append("deltas=").append(project.trajectory().deltas().size()).append('\n');
        builder.append("embeddings=").append(project.embeddings().size()).append('\n');
        builder.append("mutations=").append(project.mutationDataset().size()).append('\n');
        builder.append("registryProposals=").append(project.registryProposals().size()).append('\n');
        for (var snapshot : project.trajectory().snapshots()) {
          builder
              .append("dna:")
              .append(snapshot.commitHash())
              .append('=')
              .append(snapshot.dnaFingerprint())
              .append('\n');
        }
        for (var embedding : project.embeddings()) {
          builder
              .append("embed:")
              .append(embedding.nodeId())
              .append('=')
              .append(embedding.embeddingHex())
              .append('\n');
        }
      }
      Files.writeString(manifest, builder.toString(), StandardCharsets.UTF_8);
      return Result.ok(manifest);
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
