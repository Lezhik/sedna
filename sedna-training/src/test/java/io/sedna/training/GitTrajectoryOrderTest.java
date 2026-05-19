package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.reverse.git.GitTrajectoryStep;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitTrajectoryOrderTest {

  @Test
  void commitHashesAreOldestFirst(@TempDir Path temp) throws IOException, GitAPIException {
    Files.writeString(temp.resolve("README.md"), "v1\n");
    try (Git git = Git.init().setDirectory(temp.toFile()).call()) {
      git.add().addFilepattern(".").call();
      String firstHash = git.commit().setMessage("first").call().getName();
      Files.writeString(temp.resolve("README.md"), "v2\n");
      git.add().addFilepattern(".").call();
      String secondHash = git.commit().setMessage("second").call().getName();

      var trajectory = new GitTrajectoryStep().extract(temp);
      assertTrue(trajectory.isOk());
      assertEquals(List.of(firstHash, secondHash), trajectory.value().commitHashes());
    }
  }
}
