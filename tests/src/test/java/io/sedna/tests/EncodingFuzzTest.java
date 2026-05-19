package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.util.ArrayList;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/** Fuzz-style encoding: shuffled node order must yield identical DNA bytes. */
class EncodingFuzzTest {

  @RepeatedTest(32)
  void shuffledNodesProduceIdenticalDnaBytes(RepetitionInfo repetition) {
    int iteration = repetition.getCurrentRepetition();
    SemanticGraph canonical = CmsReferenceFixtureGraph.create();
    var encoder = DnaServices.encoder();
    var decoder = DnaServices.decoder();
    byte[] baseline = encoder.encode(canonical).value();

    var nodes = new ArrayList<>(canonical.nodes());
    java.util.Collections.rotate(nodes, (iteration - 1) % Math.max(1, nodes.size()));
    SemanticGraph shuffled = new SemanticGraph(nodes, canonical.links(), canonical.vocabularyVersion());
    assertArrayEquals(baseline, encoder.encode(shuffled).value());

    byte[] reencoded = encoder.encode(decoder.decode(baseline).value()).value();
    assertArrayEquals(baseline, reencoded);
  }
}
