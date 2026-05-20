package io.sedna.training;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.reverse.ReversePipeline;
import io.sedna.reverse.git.CommitGraphSnapshot;
import io.sedna.reverse.git.GitCommitSnapshotExtractor;
import io.sedna.training.model.SemanticSnapshot;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Builds per-commit semantic snapshots via JGit checkout (Phase 10 / 13). */
public final class GitTrajectorySnapshots {

  private final GitCommitSnapshotExtractor extractor = new GitCommitSnapshotExtractor();

  public Result<List<SemanticSnapshot>, SemanticError> capture(
      Path projectRoot, ReversePipeline reversePipeline, DnaEncoder encoder) {
    var snapshots =
        extractor.extract(
            projectRoot,
            (root, commitHash) -> {
              Result<SemanticGraph, SemanticError> graph = reversePipeline.reverseGraph(root);
              if (!graph.isOk()) {
                return graph;
              }
              return graph;
            });
    if (!snapshots.isOk()) {
      return Result.err(snapshots.error());
    }

    List<SemanticSnapshot> results = new ArrayList<>();
    for (CommitGraphSnapshot snapshot : snapshots.value()) {
      var fingerprint = DnaFingerprint.of(snapshot.graph(), encoder);
      if (!fingerprint.isOk()) {
        return Result.err(fingerprint.error());
      }
      results.add(new SemanticSnapshot(snapshot.commitHash(), snapshot.graph(), fingerprint.value()));
    }
    return Result.ok(List.copyOf(results));
  }
}
