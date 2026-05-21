package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-020 — training dataset generation via CLI. */
@Tag("e2e")
class TrainingDatasetE2eTest {

  @Test
  void trainCorpusProducesStableManifest() throws Exception {
    Path out1 = E2eTestSupport.outputDir("E2E-020-a");
    Path out2 = E2eTestSupport.outputDir("E2E-020-b");
    Path projects = E2eTestSupport.e2eTrainingProjectsManifest();
    E2eTestSupport.CliResult first =
        E2eTestSupport.runCli(
            "train", "--projects=" + projects, "--output=" + out1, "--clean", "--format=json");
    E2eTestSupport.CliResult second =
        E2eTestSupport.runCli(
            "train", "--projects=" + projects, "--output=" + out2, "--clean", "--format=json");
    assertEquals(0, first.exitCode(), () -> first.stdout() + first.stderr());
    assertEquals(0, second.exitCode());

    Path manifest1 = out1.resolve("dataset.manifest");
    Path manifest2 = out2.resolve("dataset.manifest");
    assertTrue(Files.isRegularFile(manifest1));
    assertTrue(Files.isRegularFile(out1.resolve("dataset.manifest.sha256")));
    assertEquals(Files.readString(manifest1), Files.readString(manifest2));
    assertEquals(
        Files.readString(out1.resolve("dataset.manifest.sha256")),
        Files.readString(out2.resolve("dataset.manifest.sha256")));
  }
}
