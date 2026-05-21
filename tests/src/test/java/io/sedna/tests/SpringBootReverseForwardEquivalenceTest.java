package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Phase 10: reverse(forward(dna)) for {@code examples/sedna-demo/*} (default {@code test} task).
 *
 * <p>E2E covers golden + cms-reference in {@link io.sedna.tests.e2e.ReverseForwardEquivalenceE2eTest}
 * (E2E-010).
 */
class SpringBootReverseForwardEquivalenceTest {

  @ParameterizedTest
  @ValueSource(strings = {"spring-demo", "inventory-demo", "order-demo"})
  void reverseForwardRoundTripPreservesSemantics(String fixtureName, @TempDir Path outputDir) {
    Path project = RepoPaths.exampleProject(fixtureName);

    var reverse = ReverseServices.pipeline();
    var original = reverse.reverseGraph(project);
    assertTrue(original.isOk(), () -> fixtureName + ": " + original.error());

    byte[] dna = DnaServices.encoder().encode(original.value()).value();
    Path generated = outputDir.resolve(fixtureName);
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var written = forward.runToDirectory(dna, generated);
    assertTrue(written.isOk(), () -> fixtureName + ": " + written.error());

    var roundTrip = reverse.reverseGraph(generated);
    assertTrue(roundTrip.isOk(), () -> fixtureName + ": " + roundTrip.error());

    var expandedOriginal = SednaFoldMotifCodec.INSTANCE.expand(original.value());
    assertTrue(expandedOriginal.isOk(), () -> fixtureName + ": " + expandedOriginal.error());
    var expandedRoundTrip = SednaFoldMotifCodec.INSTANCE.expand(roundTrip.value());
    assertTrue(expandedRoundTrip.isOk(), () -> fixtureName + ": " + expandedRoundTrip.error());

    var equivalent =
        SemanticEquivalenceChecker.checkEquivalent(
            expandedOriginal.value(), expandedRoundTrip.value());
    assertTrue(equivalent.isOk(), () -> fixtureName + ": " + equivalent.error());
  }
}
