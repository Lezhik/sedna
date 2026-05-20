package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.training.fixture.SyntheticSpringProjectFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Phase 13 / v1.5 acceptance gates (deterministic synthetic corpus). */
class Phase13AcceptanceTest {

  private static final int COMMITS =
      SyntheticSpringProjectFactory.PHASE13_MIN_COMMITS + 1;

  private final TrainingPipeline pipeline = TrainingServices.pipeline();

  @Test
  void identicalGitHistoryProducesIdenticalTrajectory(@TempDir Path temp)
      throws IOException, GitAPIException {
    Path project = temp.resolve("history-demo");
    String basePackage = "com.acme.history";
    SyntheticSpringProjectFactory.create(project, basePackage);
    SyntheticSpringProjectFactory.initGitHistory(project, basePackage, COMMITS);

    var first = pipeline.trainProject(project);
    var second = pipeline.trainProject(project);
    assertTrue(first.isOk(), () -> String.valueOf(first.error()));
    assertTrue(second.isOk(), () -> String.valueOf(second.error()));

    assertEquals(COMMITS, first.value().trajectory().snapshots().size());
    assertEquals(
        SemanticTrajectoryHasher.fingerprint(first.value().trajectory()),
        SemanticTrajectoryHasher.fingerprint(second.value().trajectory()));
  }

  @Test
  void trainingCorpusProcessesTwentyProjectsEndToEnd(@TempDir Path temp)
      throws IOException, GitAPIException {
    Path corpusRoot = temp.resolve("corpus");
    List<Path> projects = new ArrayList<>();
    for (int i = 0; i < SyntheticSpringProjectFactory.PHASE13_MIN_PROJECTS; i++) {
      Path project = corpusRoot.resolve("p" + String.format("%02d", i));
      String basePackage = "com.acme.corpus.p" + i;
      SyntheticSpringProjectFactory.create(project, basePackage);
      projects.add(project);
    }

    var trained = pipeline.train(projects);
    assertTrue(trained.isOk(), () -> String.valueOf(trained.error()));
    assertEquals(SyntheticSpringProjectFactory.PHASE13_MIN_PROJECTS, trained.value().projects().size());
    assertTrue(
        trained.value().datasetFingerprint().length() >= 64,
        "dataset fingerprint must be populated");
  }

  @Test
  void mutationDatasetFromGitHistoryMeetsMinimum(@TempDir Path temp)
      throws IOException, GitAPIException {
    Path corpusRoot = temp.resolve("mutation-corpus");
    int projectsWithHistory = 23;
    List<Path> projects = new ArrayList<>();
    for (int i = 0; i < projectsWithHistory; i++) {
      Path project = corpusRoot.resolve("hist-" + i);
      String basePackage = "com.acme.hist.p" + i;
      SyntheticSpringProjectFactory.create(project, basePackage);
      SyntheticSpringProjectFactory.initGitHistory(project, basePackage, COMMITS);
      projects.add(project);
    }

    var trained = pipeline.train(projects);
    assertTrue(trained.isOk(), () -> String.valueOf(trained.error()));

    long mutationRows =
        trained.value().projects().stream()
            .mapToLong(project -> project.mutationDataset().size())
            .sum();
    assertTrue(
        mutationRows >= SyntheticSpringProjectFactory.PHASE13_MIN_MUTATION_ROWS,
        () -> "mutation rows=" + mutationRows);
    trained
        .value()
        .projects()
        .forEach(
            project ->
                assertTrue(
                    project.mutationDataset().stream()
                        .allMatch(entry -> entry.label().contains(":")),
                    "history mutations must be commit-scoped"));
  }
}
