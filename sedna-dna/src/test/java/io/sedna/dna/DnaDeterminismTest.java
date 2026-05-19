package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

/** Phase 1 acceptance: byte-identical re-encode across iterations. */
class DnaDeterminismTest {

  private final DnaEncoder encoder = DnaServices.encoder();
  private final DnaDecoder decoder = DnaServices.decoder();

  @Test
  void reencodeIsByteIdenticalAcross100Iterations() {
    byte[] baseline = encoder.encode(CmsReferenceFixtureGraph.create()).value();
    for (int i = 0; i < 100; i++) {
      byte[] roundTrip = encoder.encode(decoder.decode(baseline).value()).value();
      assertArrayEquals(baseline, roundTrip, "iteration " + i);
    }
  }
}
