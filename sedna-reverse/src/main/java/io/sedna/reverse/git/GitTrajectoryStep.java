package io.sedna.reverse.git;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;

/** Step 8 — optional git trajectory extraction (MVP: commit hashes only). */
public final class GitTrajectoryStep {

  public record GitTrajectory(List<String> commitHashes) {
    public GitTrajectory {
      commitHashes = List.copyOf(commitHashes);
    }
  }

  public Result<GitTrajectory, SemanticError> extract(Path projectRoot) {
    Path gitDir = projectRoot.resolve(".git");
    if (!Files.exists(gitDir)) {
      return Result.ok(new GitTrajectory(List.of()));
    }
    try (Git git = Git.open(projectRoot.toFile())) {
      List<String> commits = new ArrayList<>();
      Iterable<RevCommit> log = git.log().call();
      for (RevCommit commit : log) {
        commits.add(commit.getId().getName());
      }
      // JGit log is newest-first; trajectories require oldest-first commit order.
      java.util.Collections.reverse(commits);
      return Result.ok(new GitTrajectory(List.copyOf(commits)));
    } catch (NoHeadException ex) {
      return Result.ok(new GitTrajectory(List.of()));
    } catch (GitAPIException | java.io.IOException ex) {
      return Result.ok(new GitTrajectory(List.of()));
    }
  }
}
