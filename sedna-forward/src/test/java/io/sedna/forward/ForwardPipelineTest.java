package io.sedna.forward;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ForwardPipelineTest {

  @Test
  void forwardWritesExpectedFiles(@TempDir Path output) throws java.io.IOException {
    byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    ForwardPipeline pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);

    var result = pipeline.runToDirectory(dna, output);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));

    assertTrue(Files.exists(output.resolve("build.gradle.kts")));
    assertTrue(Files.exists(output.resolve("src/main/java/io/sedna/cms/CmsApplication.java")));
    assertTrue(Files.exists(output.resolve("src/main/java/io/sedna/cms/domain/User.java")));
    assertTrue(Files.exists(output.resolve("src/main/java/io/sedna/cms/service/UserService.java")));
    assertTrue(Files.exists(output.resolve("src/main/java/io/sedna/cms/web/UserController.java")));
  }
}
