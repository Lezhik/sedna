package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-010 — reverse(forward(dna)) semantic equivalence. */
@Tag("e2e")
class ReverseForwardEquivalenceE2eTest {

  @Test
  void goldenFixtureReverseForwardEquivalence() throws Exception {
    byte[] dna = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    assertEquivalenceRoundTrip(dna, E2eTestSupport.outputDir("E2E-010-golden").resolve("generated"));
  }

  @Test
  void cmsReferenceProjectReverseForwardEquivalence() throws Exception {
    Path project = E2eTestSupport.exampleProject("cms-reference");
    var reverse = ReverseServices.pipeline();
    var original = reverse.reverseGraph(project);
    assertTrue(original.isOk(), () -> String.valueOf(original.error()));
    byte[] dna = DnaServices.encoder().encode(original.value()).value();
    assertEquivalenceRoundTrip(dna, E2eTestSupport.outputDir("E2E-010-cms").resolve("generated"));
  }

  private static void assertEquivalenceRoundTrip(byte[] dna, Path generated) throws Exception {
    E2eTestSupport.prepareDir(generated.getParent());
    E2eTestSupport.prepareDir(generated);
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    assertTrue(forward.runToDirectory(dna, generated).isOk());

    var reverse = ReverseServices.pipeline();
    var roundTrip = reverse.reverseGraph(generated);
    assertTrue(roundTrip.isOk(), () -> String.valueOf(roundTrip.error()));

    var baseline = SednaFoldMotifCodec.INSTANCE.expand(DnaServices.decoder().decode(dna).value());
    assertTrue(baseline.isOk());
    var expanded = SednaFoldMotifCodec.INSTANCE.expand(roundTrip.value());
    assertTrue(expanded.isOk());
    assertTrue(
        SemanticEquivalenceChecker.checkEquivalent(baseline.value(), expanded.value()).isOk());
  }
}
