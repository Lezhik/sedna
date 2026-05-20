package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-002 — DNA decode of golden fixture. */
@Tag("e2e")
class DnaDecodeE2eTest {

  @Test
  void decodeGoldenFixtureSummary() throws Exception {
    byte[] dna = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    var decoded = DnaServices.decoder().decode(dna);
    assertTrue(decoded.isOk(), () -> String.valueOf(decoded.error()));
    assertEquals(3, decoded.value().nodes().size());
    assertEquals(2, decoded.value().links().size());
    assertEquals("core:1.0", decoded.value().vocabularyVersion().canonical());
  }
}
