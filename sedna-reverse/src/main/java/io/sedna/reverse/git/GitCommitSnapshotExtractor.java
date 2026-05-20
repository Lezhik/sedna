package io.sedna.reverse.git;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/** Checks out each commit (oldest-first) and extracts semantic graphs deterministically. */
public final class GitCommitSnapshotExtractor {

  /** Maximum number of commits processed per project trajectory. */
  public static final int MAX_COMMITS = 32;

  private final GitTrajectoryStep trajectoryStep = new GitTrajectoryStep();

  /** Creates a snapshot extractor with default trajectory settings. */
  public GitCommitSnapshotExtractor() {}

  /**
   * Checks out each commit and extracts a semantic graph snapshot.
   *
   * @param projectRoot Gradle project root with a {@code .git} directory
   * @param graphAtCommit callback that builds a graph at the checked-out commit
   * @return ordered commit snapshots or structured error
   */
  public Result<List<CommitGraphSnapshot>, SemanticError> extract(
      Path projectRoot, BiFunction<Path, String, Result<SemanticGraph, SemanticError>> graphAtCommit) {
    Path gitDir = projectRoot.resolve(".git");
    if (!Files.isDirectory(gitDir)) {
      return Result.ok(List.of());
    }

    var trajectory = trajectoryStep.extract(projectRoot);
    if (!trajectory.isOk()) {
      return Result.err(trajectory.error());
    }

    List<String> commits = trajectory.value().commitHashes();
    if (commits.isEmpty()) {
      return Result.ok(List.of());
    }
    if (commits.size() > MAX_COMMITS) {
      commits = commits.subList(commits.size() - MAX_COMMITS, commits.size());
    }

    try (Git git = Git.open(projectRoot.toFile())) {
      Repository repository = git.getRepository();
      String originalHead = repository.getFullBranch();
      if (originalHead == null || originalHead.isBlank()) {
        originalHead = "HEAD";
      }

      List<CommitGraphSnapshot> snapshots = new ArrayList<>();
      try {
        for (String commitHash : commits) {
          git.checkout().setName(commitHash).setForced(true).call();
          var graph = graphAtCommit.apply(projectRoot, commitHash);
          if (!graph.isOk()) {
            return Result.err(graph.error());
          }
          snapshots.add(new CommitGraphSnapshot(commitHash, graph.value()));
        }
      } finally {
        git.checkout().setName(originalHead).setForced(true).call();
      }
      return Result.ok(List.copyOf(snapshots));
    } catch (GitAPIException | IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
