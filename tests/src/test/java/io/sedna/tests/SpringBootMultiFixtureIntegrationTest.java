package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.NodeKind;
import io.sedna.dna.DnaServices;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Phase 10: reverse + forward compile for multiple Spring Boot monolith fixtures. */
class SpringBootMultiFixtureIntegrationTest {

  private static final List<String> FIXTURES =
      List.of("spring-demo", "inventory-demo", "order-demo");

  @ParameterizedTest
  @ValueSource(strings = {"spring-demo", "inventory-demo", "order-demo"})
  void reverseProducesDna(String fixtureName) {
    Path project = RepoPaths.exampleProject(fixtureName);
    var graph = ReverseServices.pipeline().reverseGraph(project);
    assertTrue(graph.isOk(), () -> fixtureName + ": " + graph.error());
    boolean hasMotif =
        graph.value().nodes().stream().anyMatch(node -> node.kind() == NodeKind.MOTIF);
    boolean hasCrudStack =
        graph.value().nodes().stream()
                .filter(
                    node ->
                        node.kind() == NodeKind.ENTITY
                            || node.kind() == NodeKind.SERVICE
                            || node.kind() == NodeKind.CONTROLLER)
                .count()
            >= 3;
    assertTrue(hasMotif || hasCrudStack, fixtureName + " should fold to MOTIF or expose CRUD nodes");
  }

  @ParameterizedTest
  @ValueSource(strings = {"spring-demo", "inventory-demo", "order-demo"})
  void forwardGeneratedProjectCompiles(String fixtureName, @TempDir Path outputDir) throws Exception {
    Path repoRoot = RepoPaths.locateRoot();
    Path project = RepoPaths.exampleProject(fixtureName);
    Path gradlew = RepoPaths.gradlew(repoRoot);

    var reverse = ReverseServices.pipeline();
    var graph = reverse.reverseGraph(project);
    assertTrue(graph.isOk(), () -> String.valueOf(graph.error()));

    byte[] dna = DnaServices.encoder().encode(graph.value()).value();
    Path generated = outputDir.resolve(fixtureName);
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var written = forward.runToDirectory(dna, generated);
    assertTrue(written.isOk(), () -> String.valueOf(written.error()));

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
    boolean finished = process.waitFor(15, TimeUnit.MINUTES);
    assertTrue(finished, fixtureName + " build timeout\n" + log);
    assertEquals(0, process.exitValue(), fixtureName + " build failed:\n" + log);
    assertTrue(Files.exists(generated.resolve("build.gradle.kts")));
  }
}
