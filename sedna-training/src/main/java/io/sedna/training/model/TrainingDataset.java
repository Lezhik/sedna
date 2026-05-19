package io.sedna.training.model;

import java.util.List;

/** Multi-project training dataset (projects never merged). */
public record TrainingDataset(List<TrainingProjectResult> projects, String datasetFingerprint) {
  public TrainingDataset {
    projects = List.copyOf(projects);
    if (datasetFingerprint == null || datasetFingerprint.isBlank()) {
      throw new IllegalArgumentException("datasetFingerprint required");
    }
  }
}
