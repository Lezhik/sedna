package io.sedna.reverse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ReverseSpringDemoTest {

  private static final Path SPRING_DEMO =
      Paths.get("..", "examples", "spring-demo").normalize().toAbsolutePath();

  @Test
  void reverseSpringDemoProducesValidGraph() {
    var pipeline = ReverseServices.pipeline();
    var graph = pipeline.reverseGraph(SPRING_DEMO);
    assertTrue(graph.isOk(), () -> String.valueOf(graph.error()));
    assertTrue(
        graph.value().nodes().stream()
            .anyMatch(
                node ->
                    node.constraints().stream()
                        .anyMatch(c -> c.code().startsWith("SOURCE_PACKAGE:com.acme.demo"))));
  }

  @Test
  void forwardFromSpringDemoDnaGeneratesProjectFiles() {
    var reverse = ReverseServices.pipeline();
    var graph = reverse.reverseGraph(SPRING_DEMO);
    assertTrue(graph.isOk(), () -> String.valueOf(graph.error()));

    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var encoded = io.sedna.dna.DnaServices.encoder().encode(graph.value());
    var generated = forward.run(encoded.value());
    assertTrue(generated.isOk(), () -> String.valueOf(generated.error()));
    assertTrue(generated.value().files().containsKey("build.gradle.kts"));
    assertTrue(
        generated.value().files().keySet().stream()
            .anyMatch(path -> path.contains("com/acme/demo/web/ItemController.java")));
  }
}
