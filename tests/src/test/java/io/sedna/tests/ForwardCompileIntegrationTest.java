package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForwardCompileIntegrationTest {

  @Test
  void forwardFixtureCompilesWithGradle(@TempDir Path outputDir) throws Exception {
    Path repoRoot = RepoPaths.locateRoot();
    Path gradlew = RepoPaths.gradlew(repoRoot);
    Path fixture = io.sedna.core.examples.ExamplesLayout.goldenCmsFixture(repoRoot);
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

}
