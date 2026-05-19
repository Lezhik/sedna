package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.NodeKind;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class SednaFoldMotifCodecTest {

  private final SednaFoldMotifCodec codec = SednaFoldMotifCodec.INSTANCE;
  private final DnaEncoder encoder = DnaServices.encoder();

  @Test
  void foldReducesCmsReferenceNodeCount() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var folded = codec.fold(graph);
    assertTrue(folded.isOk());
    assertEquals(3, graph.nodes().size());
    assertEquals(1, folded.value().nodes().size());
    assertEquals(NodeKind.MOTIF, folded.value().nodes().getFirst().kind());
  }

  @Test
  void foldExpandFoldProducesStableDnaBytes() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var folded = codec.fold(graph);
    assertTrue(folded.isOk());
    var expanded = codec.expand(folded.value());
    assertTrue(expanded.isOk());
    var refolded = codec.fold(expanded.value());
    assertTrue(refolded.isOk());

    byte[] first = encoder.encode(refolded.value()).value();
    byte[] second = encoder.encode(codec.fold(expanded.value()).value()).value();
    assertArrayEquals(first, second);
  }

  @Test
  void expandRestoresMemberNodeCount() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var folded = codec.fold(graph);
    var expanded = codec.expand(folded.value());
    assertTrue(expanded.isOk());
    assertEquals(3, expanded.value().nodes().size());
    assertEquals(2, expanded.value().links().size());
  }
}
