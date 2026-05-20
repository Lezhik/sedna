package io.sedna.training;

import io.sedna.training.model.TrainingDataset;

/** Canonical manifest text for a training dataset (checksum input). */
public final class TrainingManifestBuilder {

  private TrainingManifestBuilder() {}

  public static String build(TrainingDataset dataset) {
    StringBuilder builder = new StringBuilder();
    builder.append("sedna-training-dataset-v1").append('\n');
    builder.append("fingerprint=").append(dataset.datasetFingerprint()).append('\n');
    builder.append("projects=").append(dataset.projects().size()).append('\n');
    for (var project : dataset.projects()) {
      builder.append("[project]").append('\n');
      builder.append("path=").append(project.projectPath()).append('\n');
      builder.append("trajectory=").append(SemanticTrajectoryHasher.fingerprint(project.trajectory())).append('\n');
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
    return builder.toString();
  }
}
