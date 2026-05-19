package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForwardCompileIntegrationTest {

  @Test
  void forwardFixtureCompilesWithGradle(@TempDir Path outputDir) throws Exception {
    Path repoRoot = locateRepoRoot();
    Path gradlew =
        repoRoot.resolve(System.getProperty("os.name").toLowerCase().contains("win") ? "gradlew.bat" : "gradlew");
    Path fixture = repoRoot.resolve("examples/cms-reference-fixture.sdna");
    Path generated = outputDir.resolve("generated");

    byte[] dna = Files.readAllBytes(fixture);
    var pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var result = pipeline.runToDirectory(dna, generated);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));

    ProcessBuilder builder =
        new ProcessBuilder(
            gradlew.toAbsolutePath().toString(),
            "-p",
            generated.toAbsolutePath().toString(),
            "build",
            "--no-daemon");
    builder.directory(repoRoot.toFile());
    builder.redirectErrorStream(true);
    Process process = builder.start();
    String log = new String(process.getInputStream().readAllBytes());
    boolean finished = process.waitFor(10, TimeUnit.MINUTES);
    assertTrue(finished, "Gradle build did not finish in time\n" + log);
    assertEquals(0, process.exitValue(), "Gradle build failed:\n" + log);
  }

  private static Path locateRepoRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if (Files.exists(cwd.resolve("examples/cms-reference-fixture.sdna"))) {
      return cwd;
    }
    Path parent = cwd.resolve("..").normalize();
    if (Files.exists(parent.resolve("examples/cms-reference-fixture.sdna"))) {
      return parent;
    }
    throw new IllegalStateException("Cannot locate repository root from " + cwd);
  }
}
