package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrainingPipelineTest {

  private static final Path CMS_REFERENCE =
      Paths.get("..", "examples", "cms-reference").toAbsolutePath().normalize();

  private final TrainingPipeline pipeline = TrainingServices.pipeline();

  @Test
  void trainsCmsReferenceProject() {
    var result = pipeline.trainProject(CMS_REFERENCE);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));
    var project = result.value();
    assertEquals(1, project.embeddings().size());
    assertFalse(project.mutationDataset().isEmpty());
    assertFalse(project.registryProposals().isEmpty());
    assertEquals(1, project.trajectory().snapshots().size());
  }

  @Test
  void identicalRunsProduceIdenticalDatasetFingerprint(@TempDir Path temp) throws IOException {
    Path listFile = temp.resolve("projects.txt");
    java.nio.file.Files.writeString(listFile, CMS_REFERENCE.toString() + System.lineSeparator());

    var loader = new ProjectListLoader();
    List<Path> projects = loader.load(listFile).value();

    var first = pipeline.train(projects);
    var second = pipeline.train(projects);
    assertTrue(first.isOk());
    assertTrue(second.isOk());
    assertEquals(first.value().datasetFingerprint(), second.value().datasetFingerprint());
    assertEquals(first.value().projects().getFirst().embeddings(), second.value().projects().getFirst().embeddings());
  }

  @Test
  void writesDeterministicManifest(@TempDir Path temp) throws IOException {
    Path listFile = temp.resolve("projects.txt");
    java.nio.file.Files.writeString(listFile, CMS_REFERENCE.toString() + System.lineSeparator());
    List<Path> projects = new ProjectListLoader().load(listFile).value();
    var dataset = pipeline.train(projects).value();

    Path out = temp.resolve("out");
    var written = new TrainingDatasetWriter().write(dataset, out);
    assertTrue(written.isOk());
    String manifest = java.nio.file.Files.readString(written.value().manifestPath());
    String checksum = java.nio.file.Files.readString(written.value().manifestChecksumPath()).trim();
    String report = java.nio.file.Files.readString(written.value().reproducibilityReportPath());
    var again = new TrainingDatasetWriter().write(dataset, temp.resolve("out2"));
    String manifest2 = java.nio.file.Files.readString(again.value().manifestPath());
    assertEquals(manifest, manifest2);
    assertEquals(checksum, java.nio.file.Files.readString(again.value().manifestChecksumPath()).trim());
    assertEquals(report, java.nio.file.Files.readString(again.value().reproducibilityReportPath()));
    assertTrue(report.contains("manifestSha256=" + checksum));
  }
}
