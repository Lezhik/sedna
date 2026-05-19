package io.sedna.tests;

import java.nio.file.Files;
import java.nio.file.Path;

final class RepoPaths {

  private RepoPaths() {}

  static Path locateRoot() {
    Path cwd = Path.of("").toAbsolutePath();
    if (Files.exists(cwd.resolve("examples/cms-reference-fixture.sdna"))) {
      return cwd;
    }
    Path parent = cwd.resolve("..").normalize();
    if (Files.exists(parent.resolve("examples/cms-reference-fixture.sdna"))) {
      return parent;
    }
    throw new IllegalStateException("Cannot locate repository root from " + cwd);
  }

  static Path gradlew(Path repoRoot) {
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    return repoRoot.resolve(windows ? "gradlew.bat" : "gradlew");
  }
}
