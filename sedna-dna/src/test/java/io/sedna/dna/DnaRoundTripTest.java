package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** FR-dna.02 / FR-dna.03 round-trip and byte identity tests. */
class DnaRoundTripTest {

  private final DnaEncoder encoder = DnaServices.encoder();
  private final DnaDecoder decoder = DnaServices.decoder();

  @Test
  void encodeDecodeRoundTrip() {
    SemanticGraph original = CanonicalOrdering.canonicalize(sampleGraph());
    byte[] dna = encoder.encode(original).value();
    SemanticGraph decoded = decoder.decode(dna).value();
    assertEquals(original.nodes().size(), decoded.nodes().size());
    assertEquals(original.links().size(), decoded.links().size());
    assertEquals(
        original.nodes().stream().map(GenomeNode::nodeId).toList(),
        decoded.nodes().stream().map(GenomeNode::nodeId).toList());
    assertEquals(original, decoded);
  }

  @Test
  void canonicallyEqualGraphsProduceIdenticalBytes() {
    SemanticGraph graphA = sampleGraph();
    List<GenomeNode> shuffled = new ArrayList<>(graphA.nodes());
    java.util.Collections.reverse(shuffled);
    SemanticGraph graphB = new SemanticGraph(shuffled, graphA.links(), graphA.vocabularyVersion());

    byte[] dnaA = encoder.encode(graphA).value();
    byte[] dnaB = encoder.encode(graphB).value();
    assertArrayEquals(dnaA, dnaB);
  }

  @Test
  void reencodeIsByteIdentical() {
    SemanticGraph graph = sampleGraph();
    byte[] first = encoder.encode(graph).value();
    SemanticGraph decoded = decoder.decode(first).value();
    byte[] second = encoder.encode(decoded).value();
    assertArrayEquals(first, second);
  }

  @Test
  void invalidMagicFails() {
    Result<SemanticGraph, SemanticError> result = decoder.decode(new byte[] {0, 1, 2, 3, 4, 5});
    assertTrue(!result.isOk());
    assertTrue(result.error().message().toLowerCase(Locale.ROOT).contains("magic"));
  }

  private static SemanticGraph sampleGraph() {
    return io.sedna.dna.fixture.CmsReferenceFixtureGraph.create();
  }
}
