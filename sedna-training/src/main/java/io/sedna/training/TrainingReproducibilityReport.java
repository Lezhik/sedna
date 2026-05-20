package io.sedna.training;

import io.sedna.training.model.TrainingDataset;

/** Deterministic reproducibility report for a multi-project training run. */
public final class TrainingReproducibilityReport {

  private TrainingReproducibilityReport() {}

  /**
   * Builds a reproducibility report with dataset and index fingerprints.
   *
   * @param dataset training dataset
   * @param manifestBody manifest text
   * @param index embedding index built from the dataset
   * @return reproducibility report text
   */
  public static String build(TrainingDataset dataset, String manifestBody, SemanticEmbeddingIndex index) {
    StringBuilder builder = new StringBuilder();
    builder.append("sedna-reproducibility-v1").append('\n');
    builder.append("datasetFingerprint=").append(dataset.datasetFingerprint()).append('\n');
    builder.append("manifestSha256=").append(TrainingManifestHasher.sha256(manifestBody)).append('\n');
    builder.append("embeddingIndexSha256=").append(index.fingerprint()).append('\n');
    builder.append("embeddingRows=").append(index.size()).append('\n');
    builder.append("retrieval=PURE_JAVA_BRUTE_FORCE_COSINE").append('\n');
    for (var project : dataset.projects()) {
      builder
          .append("projectTrajectory=")
          .append(project.projectPath())
          .append('=')
          .append(SemanticTrajectoryHasher.fingerprint(project.trajectory()))
          .append('\n');
    }
    builder.append("status=DETERMINISTIC_REPLAY_READY").append('\n');
    return builder.toString();
  }
}
