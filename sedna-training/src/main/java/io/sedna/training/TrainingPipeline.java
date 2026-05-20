package io.sedna.training;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.dna.DnaEncoder;
import io.sedna.registry.SemanticRegistry;
import io.sedna.reverse.ReversePipeline;
import io.sedna.reverse.git.GitTrajectoryStep;
import io.sedna.training.model.SemanticSnapshot;
import io.sedna.training.model.SemanticTrajectory;
import io.sedna.training.model.TrainingDataset;
import io.sedna.training.model.TrainingProjectResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Phase 6 training pipeline: per-project folders, never cross-project merge. */
public final class TrainingPipeline {

  private final ReversePipeline reversePipeline;
  private final DnaEncoder encoder;
  private final GitTrajectoryStep gitTrajectoryStep = new GitTrajectoryStep();
  private final GitTrajectorySnapshots gitTrajectorySnapshots = new GitTrajectorySnapshots();
  private final TrajectoryBuilder trajectoryBuilder = new TrajectoryBuilder();
  private final DeterministicSemanticEmbedder embedder = new DeterministicSemanticEmbedder();
  private final MutationDatasetGenerator mutationDatasetGenerator = new MutationDatasetGenerator();
  private final RegistryUpdateProposer registryUpdateProposer;

  public TrainingPipeline(ReversePipeline reversePipeline, DnaEncoder encoder, SemanticRegistry registry) {
    this.reversePipeline = reversePipeline;
    this.encoder = encoder;
    this.registryUpdateProposer = new RegistryUpdateProposer(registry);
  }

  public Result<TrainingDataset, SemanticError> train(List<Path> projectPaths) {
    List<Path> ordered =
        projectPaths.stream()
            .map(path -> path.toAbsolutePath().normalize())
            .sorted(Comparator.comparing(Path::toString))
            .toList();

    List<TrainingProjectResult> results = new ArrayList<>();
    for (Path project : ordered) {
      var trained = trainProject(project);
      if (!trained.isOk()) {
        return Result.err(trained.error());
      }
      results.add(trained.value());
    }

    TrainingDataset pending = new TrainingDataset(results, "pending");
    return Result.ok(
        new TrainingDataset(pending.projects(), TrainingDatasetHasher.fingerprint(pending)));
  }

  public Result<TrainingProjectResult, SemanticError> trainProject(Path projectRoot) {
    var graph = reversePipeline.reverseGraph(projectRoot);
    if (!graph.isOk()) {
      return Result.err(graph.error());
    }

    var fingerprint = DnaFingerprint.of(graph.value(), encoder);
    if (!fingerprint.isOk()) {
      return Result.err(fingerprint.error());
    }

    var git = gitTrajectoryStep.extract(projectRoot);
    if (!git.isOk()) {
      return Result.err(git.error());
    }

    List<String> commitOrder = git.value().commitHashes();
    List<SemanticSnapshot> snapshots = new ArrayList<>();
    if (commitOrder.size() >= 2) {
      var perCommit = gitTrajectorySnapshots.capture(projectRoot, reversePipeline, encoder);
      if (!perCommit.isOk()) {
        return Result.err(perCommit.error());
      }
      snapshots = perCommit.value();
    }
    if (snapshots.isEmpty()) {
      String snapshotCommit = commitOrder.isEmpty() ? "WORKTREE" : commitOrder.getLast();
      snapshots = List.of(new SemanticSnapshot(snapshotCommit, graph.value(), fingerprint.value()));
    }

    SemanticTrajectory trajectory = trajectoryBuilder.build(projectRoot, commitOrder, snapshots);

    return Result.ok(
        new TrainingProjectResult(
            projectRoot.toString(),
            trajectory,
            embedder.embed(graph.value()),
            mutationDatasetGenerator.generate(graph.value()),
            registryUpdateProposer.propose(graph.value())));
  }
}
