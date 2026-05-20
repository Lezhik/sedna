package io.sedna.tests;

import io.sedna.core.examples.ExamplesLayout;
import java.nio.file.Files;
import java.nio.file.Path;

final class RepoPaths {

  private RepoPaths() {}

  static Path locateRoot() {
    Path cwd = Path.of("").toAbsolutePath();
    if (Files.exists(cwd.resolve(ExamplesLayout.GOLDEN_CMS_FIXTURE))) {
      return cwd;
    }
    Path parent = cwd.resolve("..").normalize();
    if (Files.exists(parent.resolve(ExamplesLayout.GOLDEN_CMS_FIXTURE))) {
      return parent;
    }
    throw new IllegalStateException("Cannot locate repository root from " + cwd);
  }

  static Path exampleProject(String projectFolderName) {
    Path root = locateRoot();
    return ExamplesLayout.findProjectRoot(root, projectFolderName)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing example project "
                        + projectFolderName
                        + " under examples/sedna-*"));
  }

  static Path gradlew(Path repoRoot) {
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    return repoRoot.resolve(windows ? "gradlew.bat" : "gradlew");
  }
}
