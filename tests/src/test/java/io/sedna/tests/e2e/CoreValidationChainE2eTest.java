package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Validation chain §15: encode → decode → forward → compile → reverse → equivalence. */
@Tag("e2e")
class CoreValidationChainE2eTest {

  @Test
  void fullChainPreservesSemantics() throws Exception {
    Path base = E2eTestSupport.outputDir("E2E-CHAIN");
    E2eTestSupport.prepareDir(base);

    byte[] encoded = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    assertEquals(E2eTestSupport.GOLDEN_SHA256, E2eTestSupport.sha256(encoded));

    byte[] roundTrip = DnaServices.encoder().encode(DnaServices.decoder().decode(encoded).value()).value();
    assertArrayEquals(encoded, roundTrip);

    Path generated = base.resolve("generated");
    E2eTestSupport.CliResult forward =
        E2eTestSupport.runCli(
            "forward",
            "--input=" + E2eTestSupport.readGoldenFixture(),
            "--output=" + generated,
            "--clean");
    assertEquals(0, forward.exitCode());

    E2eTestSupport.runGradleBuild(E2eTestSupport.repoRoot(), generated);

    Path reversed = base.resolve("reversed.sdna");
    E2eTestSupport.CliResult reverse =
        E2eTestSupport.runCli(
            "reverse", "--input=" + generated, "--output=" + reversed, "--clean");
    assertEquals(0, reverse.exitCode());
    assertTrue(Files.size(reversed) > 0);

    var reversePipeline = ReverseServices.pipeline();
    var expandedOriginal =
        SednaFoldMotifCodec.INSTANCE.expand(DnaServices.decoder().decode(encoded).value());
    assertTrue(expandedOriginal.isOk());
    var expandedRoundTrip =
        SednaFoldMotifCodec.INSTANCE.expand(
            DnaServices.decoder().decode(Files.readAllBytes(reversed)).value());
    assertTrue(expandedRoundTrip.isOk());
    assertTrue(
        SemanticEquivalenceChecker.checkEquivalent(
                expandedOriginal.value(), expandedRoundTrip.value())
            .isOk());

    var forwardApi =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    assertTrue(forwardApi.run(encoded).isOk());
    assertTrue(
        reversePipeline.reverseGraph(E2eTestSupport.exampleProject("cms-reference")).isOk());
  }
}
