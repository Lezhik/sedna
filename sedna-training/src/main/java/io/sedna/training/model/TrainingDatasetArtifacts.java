package io.sedna.training.model;

import java.nio.file.Path;

/**
 * Output paths for a persisted training dataset directory.
 *
 * @param manifestPath {@code dataset.manifest} file path
 * @param manifestChecksumPath {@code dataset.manifest.sha256} file path
 * @param reproducibilityReportPath {@code reproducibility.report} file path
 */
public record TrainingDatasetArtifacts(
    Path manifestPath, Path manifestChecksumPath, Path reproducibilityReportPath) {}
