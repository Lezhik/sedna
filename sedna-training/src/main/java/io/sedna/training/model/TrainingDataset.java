package io.sedna.training.model;

import java.util.List;

/**
 * Multi-project training dataset (projects never merged).
 *
 * @param projects per-project training results
 * @param datasetFingerprint canonical SHA-256 fingerprint of the full dataset
 */
public record TrainingDataset(List<TrainingProjectResult> projects, String datasetFingerprint) {

  /** Defensive copy and validation. */
  public TrainingDataset {
    projects = List.copyOf(projects);
    if (datasetFingerprint == null || datasetFingerprint.isBlank()) {
      throw new IllegalArgumentException("datasetFingerprint required");
    }
  }
}
