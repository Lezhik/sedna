package io.sedna.tests.e2e;

import io.sedna.core.examples.ExamplesLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Assertions;

/** Shared utilities for SEDNA E2E tests (isolated dirs, CLI, hashes). */
public final class E2eTestSupport {

  public static final String GOLDEN_SHA256 =
      "6d75d8431baaac07398e39448b19db34b62cc6df7eb84cbdc1484d5c2d7ed8f5";

  private E2eTestSupport() {}

  public static Path repoRoot() {
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

  /** CMS reference Spring Boot tree for E2E reverse/forward (under {@code examples/sedna-e2e-tests}). */
  public static Path e2eCmsReferenceProject() {
    Path project = ExamplesLayout.e2eCmsReferenceProject(repoRoot());
    if (!java.nio.file.Files.isDirectory(project.resolve("src/main/java"))) {
      throw new IllegalStateException("Missing E2E CMS project: " + project);
    }
    return project;
  }

  /** Manifest for E2E training (projects relative to {@code sedna-e2e-tests}). */
  public static Path e2eTrainingProjectsManifest() {
    return ExamplesLayout.e2eTrainingProjectsManifest(repoRoot());
  }

  public static Path exampleProject(String projectFolderName) {
    return ExamplesLayout.findProjectRoot(repoRoot(), projectFolderName)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing example project "
                        + projectFolderName
                        + " under examples/sedna-*"));
  }

  public static Path gradlew(Path repoRoot) {
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    return repoRoot.resolve(windows ? "gradlew.bat" : "gradlew");
  }

  public static Path outputDir(String testId) {
    return repoRoot().resolve("build/test-outputs").resolve(testId).toAbsolutePath().normalize();
  }

  public static Path fixturesRoot() {
    return repoRoot().resolve("tests/fixtures").toAbsolutePath().normalize();
  }

  public static Path readGoldenFixture() {
    return ExamplesLayout.goldenCmsFixture(repoRoot());
  }

  /** Deletes and recreates {@code dir}. Prefer CLI {@code --clean} for command output trees. */
  public static void prepareDir(Path dir) throws IOException {
    if (Files.exists(dir)) {
      deleteRecursive(dir);
    }
    Files.createDirectories(dir);
  }

  public static void assertEnvLlmDisabled() {
    String enabled = System.getenv("SEDNA_LLM_ENABLED");
    Assertions.assertFalse(
        "true".equalsIgnoreCase(enabled),
        "E2E tests require SEDNA_LLM_ENABLED=false (was: " + enabled + ")");
  }

  public static CliResult runCli(String... args) throws IOException, InterruptedException {
    assertEnvLlmDisabled();
    Path root = repoRoot();
    String cliArgs = String.join(" ", args);
    ProcessBuilder builder =
        new ProcessBuilder(
            gradlew(root).toAbsolutePath().toString(),
            ":sedna-cli:run",
            "--args=" + cliArgs);
    builder.directory(root.toFile());
    builder.redirectErrorStream(true);
    Process process = builder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.MINUTES);
    if (!finished) {
      process.destroyForcibly();
      throw new IllegalStateException("CLI timed out:\n" + output);
    }
    return new CliResult(process.exitValue(), extractCliOutput(output), "");
  }

  /** Strips Gradle task noise; keeps Sedna CLI JSON or semantic lines only. */
  static String extractCliOutput(String raw) {
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("{") && trimmed.contains("\"status\"")) {
        return trimmed;
      }
    }
    StringBuilder cli = new StringBuilder();
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();
      if (isGradleNoise(trimmed)) {
        continue;
      }
      if (!trimmed.isEmpty()) {
        if (!cli.isEmpty()) {
          cli.append('\n');
        }
        cli.append(line);
      }
    }
    return cli.toString();
  }

  private static boolean isGradleNoise(String trimmed) {
    return trimmed.startsWith("> Task")
        || trimmed.startsWith("BUILD ")
        || trimmed.startsWith("FAILURE:")
        || trimmed.startsWith("Execution failed")
        || trimmed.startsWith("* What went wrong")
        || trimmed.startsWith("* Try:")
        || trimmed.startsWith("> Process ")
        || trimmed.startsWith("> Run with")
        || trimmed.startsWith("> Get more help")
        || trimmed.contains("actionable tasks:")
        || trimmed.startsWith("Starting a Gradle Daemon")
        || trimmed.startsWith("Deprecated ")
        || trimmed.startsWith("For more on this");
  }

  public static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(bytes);
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static String treeHash(Map<String, String> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      TreeMap<String, String> ordered = new TreeMap<>(files);
      for (Map.Entry<String, String> entry : ordered.entrySet()) {
        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void runGradleBuild(Path repoRoot, Path projectDir)
      throws IOException, InterruptedException {
    ProcessBuilder builder =
        new ProcessBuilder(
            gradlew(repoRoot).toAbsolutePath().toString(),
            "-p",
            projectDir.toAbsolutePath().toString(),
            "build",
            "--no-daemon");
    builder.directory(repoRoot.toFile());
    builder.redirectErrorStream(true);
    Process process = builder.start();
    String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
    if (!finished) {
      throw new IllegalStateException("Gradle build timed out:\n" + log);
    }
    if (process.exitValue() != 0) {
      throw new IllegalStateException("Gradle build failed (exit " + process.exitValue() + "):\n" + log);
    }
  }

  private static void deleteRecursive(Path root) throws IOException {
    if (Files.isDirectory(root)) {
      try (var children = Files.list(root)) {
        for (Path child : children.toList()) {
          deleteRecursive(child);
        }
      }
    }
    Files.deleteIfExists(root);
  }

  public record CliResult(int exitCode, String stdout, String stderr) {
    public String combined() {
      return stdout + stderr;
    }
  }
}
