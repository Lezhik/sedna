package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-001 — DNA encode produces golden SHA-256. */
@Tag("e2e")
class DnaEncodeE2eTest {

  @Test
  void encodeMatchesGoldenFixtureSha() throws Exception {
    Path out = E2eTestSupport.outputDir("E2E-001");
    E2eTestSupport.prepareDir(out);

    byte[] encoded = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    Path written = out.resolve("cms-encoded.sdna");
    Files.write(written, encoded);

    byte[] onDisk = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    assertArrayEquals(onDisk, encoded);
    assertEquals(E2eTestSupport.GOLDEN_SHA256, E2eTestSupport.sha256(encoded));
    assertTrue(Files.isRegularFile(written));
  }
}
