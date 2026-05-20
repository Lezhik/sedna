package io.sedna.training.model;

import java.nio.file.Path;

/** Output paths for a persisted training dataset directory. */
public record TrainingDatasetArtifacts(
    Path manifestPath, Path manifestChecksumPath, Path reproducibilityReportPath) {}
