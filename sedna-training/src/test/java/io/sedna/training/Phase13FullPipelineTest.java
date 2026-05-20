package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Phase 13 end-to-end: corpus train, manifest checksums, reproducibility report, embedding index. */
class Phase13FullPipelineTest {

  private static final Path REPO_ROOT =
      Paths.get("..").toAbsolutePath().normalize();

  @Test
  void corpusTrainWritesChecksumsAndReproducibilityReport(@TempDir Path output) {
    var trained = TrainingServices.pipeline().trainCorpus(REPO_ROOT);
    assertTrue(trained.isOk(), () -> String.valueOf(trained.error()));

    var written = new TrainingDatasetWriter().write(trained.value(), output);
    assertTrue(written.isOk(), () -> String.valueOf(written.error()));

    var artifacts = written.value();
    assertTrue(Files.exists(artifacts.manifestPath()));
    assertTrue(Files.exists(artifacts.manifestChecksumPath()));
    assertTrue(Files.exists(artifacts.reproducibilityReportPath()));

    SemanticEmbeddingIndex index = SemanticEmbeddingIndex.fromDataset(trained.value());
    assertTrue(index.size() >= trained.value().projects().size());

    String manifest = read(artifacts.manifestPath());
    String checksum = read(artifacts.manifestChecksumPath()).trim();
    String report = read(artifacts.reproducibilityReportPath());

    assertEquals(TrainingManifestHasher.sha256(manifest), checksum);
    assertTrue(report.contains("manifestSha256=" + checksum));
    assertTrue(report.contains("embeddingIndexSha256=" + index.fingerprint()));
    assertTrue(report.contains("status=DETERMINISTIC_REPLAY_READY"));

    var second = TrainingServices.pipeline().trainCorpus(REPO_ROOT);
    assertTrue(second.isOk());
    assertEquals(trained.value().datasetFingerprint(), second.value().datasetFingerprint());
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
