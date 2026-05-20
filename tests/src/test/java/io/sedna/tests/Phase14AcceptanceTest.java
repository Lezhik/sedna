package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.validation.viz.GraphvizSemanticGraphExporter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Phase 14 acceptance: visualization on cms-reference round-trip. */
class Phase14AcceptanceTest {

  @Test
  void cmsRoundTripGraphvizExport(@TempDir Path temp) throws Exception {
    Path project = RepoPaths.exampleProject("cms-reference");
    var reverse = ReverseServices.pipeline();
    var original = reverse.reverseGraph(project);
    assertTrue(original.isOk());

    byte[] dna = DnaServices.encoder().encode(original.value()).value();
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    Path generated = temp.resolve("generated");
    assertTrue(forward.runToDirectory(dna, generated).isOk());

    var roundTrip = reverse.reverseGraph(generated);
    assertTrue(roundTrip.isOk());

    String dot = GraphvizSemanticGraphExporter.toDot(roundTrip.value(), "cms-roundtrip");
    Path dotFile = temp.resolve("cms-roundtrip.dot");
    Files.writeString(dotFile, dot);
    assertTrue(Files.exists(dotFile));
    assertTrue(dot.contains("digraph cms_roundtrip"));
    assertEquals(dot, GraphvizSemanticGraphExporter.toDot(roundTrip.value(), "cms-roundtrip"));
  }
}
