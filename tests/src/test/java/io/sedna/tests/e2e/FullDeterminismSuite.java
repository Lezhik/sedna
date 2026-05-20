package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.runtime.RuntimeServices;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-031 — aggregated platform determinism gates. */
@Tag("e2e")
class FullDeterminismSuite {

  @Test
  void goldenShaForwardHashValidateAndReplay() throws Exception {
    byte[] golden = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    assertEquals(E2eTestSupport.GOLDEN_SHA256, E2eTestSupport.sha256(golden));

    byte[] encoded = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    assertArrayEquals(golden, encoded);

    var pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var first = pipeline.run(golden);
    var second = pipeline.run(golden);
    assertTrue(first.isOk() && second.isOk());
    assertEquals(
        E2eTestSupport.treeHash(first.value().files()),
        E2eTestSupport.treeHash(second.value().files()));

    E2eTestSupport.CliResult validate =
        E2eTestSupport.runCli(
            "validate", "--input=" + E2eTestSupport.readGoldenFixture(), "--format=json");
    assertEquals(0, validate.exitCode());

    Path checkpoints = E2eTestSupport.outputDir("E2E-031").resolve("checkpoints");
    E2eTestSupport.prepareDir(checkpoints.getParent());
    E2eTestSupport.prepareDir(checkpoints);
    var store = new io.sedna.persistence.FileCheckpointStore(checkpoints);
    var engine = RuntimeServices.engine(store);
    var graph = DnaServices.decoder().decode(golden).value();
    var trace = engine.run(graph);
    assertTrue(trace.isOk());
    var replay = RuntimeServices.replayHarness(store).verifyReplayMatches(trace.value());
    assertTrue(replay.isOk(), () -> String.valueOf(replay.error()));
  }
}
