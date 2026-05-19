package io.sedna.reverse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReverseForwardEquivalenceTest {

  @Test
  void reverseForwardRoundTripPreservesSemantics(@TempDir Path tempDir) {
    SemanticGraph fixture = CmsReferenceFixtureGraph.create();
    byte[] dna = DnaServices.encoder().encode(fixture).value();

    Path generated = tempDir.resolve("generated");
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var written = forward.runToDirectory(dna, generated);
    assertTrue(written.isOk(), () -> String.valueOf(written.error()));

    var reversed = ReverseServices.pipeline().reverseGraph(generated);
    assertTrue(reversed.isOk(), () -> String.valueOf(reversed.error()));

    var equivalent = SemanticEquivalenceChecker.checkEquivalent(fixture, reversed.value());
    assertTrue(equivalent.isOk(), () -> String.valueOf(equivalent.error()));
  }
}
