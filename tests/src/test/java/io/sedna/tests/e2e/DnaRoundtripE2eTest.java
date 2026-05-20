package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-003 — encode(decode(x)) byte identity across repeated runs. */
@Tag("e2e")
class DnaRoundtripE2eTest {

  @Test
  void tripleRoundTripIsByteIdentical() {
    byte[] original = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    byte[] current = original;
    for (int i = 0; i < 3; i++) {
      var decoded = DnaServices.decoder().decode(current);
      current = DnaServices.encoder().encode(decoded.value()).value();
      assertArrayEquals(original, current);
    }
  }
}
