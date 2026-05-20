package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-007 — generated forward project compiles with Gradle. */
@Tag("e2e")
class ForwardCompileE2eTest {

  @Test
  void forwardFixtureCompilesWithGradle() throws Exception {
    Path repoRoot = E2eTestSupport.repoRoot();
    Path generated = E2eTestSupport.outputDir("E2E-007").resolve("generated");
    E2eTestSupport.prepareDir(generated.getParent());
    E2eTestSupport.prepareDir(generated);

    byte[] dna = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    var pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var result = pipeline.runToDirectory(dna, generated);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));

    E2eTestSupport.runGradleBuild(repoRoot, generated);
  }
}
