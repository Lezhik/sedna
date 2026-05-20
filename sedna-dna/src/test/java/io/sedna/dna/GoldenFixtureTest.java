package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.SemanticGraph;
import io.sedna.core.examples.ExamplesLayout;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Golden-byte fixture for Phase 2 forward and Phase 3 equivalence tests. */
class GoldenFixtureTest {

  private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();
  private static final Path FIXTURE_PATH = ExamplesLayout.goldenCmsFixture(REPO_ROOT);
  private static final Path FIXTURE_DOC_PATH = ExamplesLayout.goldenCmsFixtureDoc(REPO_ROOT);

  private final DnaEncoder encoder = DnaServices.encoder();
  private final DnaDecoder decoder = DnaServices.decoder();

  @Test
  void cmsReferenceFixtureRoundTripMatchesGoldenFile() throws IOException {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    byte[] encoded = encoder.encode(graph).value();

    if (!Files.exists(FIXTURE_PATH)) {
      Path parent = FIXTURE_PATH.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(FIXTURE_PATH, encoded);
    }

    byte[] golden = Files.readAllBytes(FIXTURE_PATH);
    assertArrayEquals(
        golden,
        encoded,
        "Regenerate " + ExamplesLayout.GOLDEN_CMS_FIXTURE + " if intentional");

    SemanticGraph decoded = decoder.decode(golden).value();
    byte[] reencoded = encoder.encode(decoded).value();
    assertArrayEquals(golden, reencoded);
  }

  @Test
  void goldenSha256DocumentedInReadme() throws IOException {
    if (!Files.exists(FIXTURE_PATH)) {
      return;
    }
    byte[] golden = Files.readAllBytes(FIXTURE_PATH);
    String sha256 = sha256Hex(golden);
    assertTrue(Files.exists(FIXTURE_DOC_PATH), "Missing fixture doc at " + FIXTURE_DOC_PATH);
    String content = Files.readString(FIXTURE_DOC_PATH);
    assertTrue(
        content.contains(sha256),
        "Update " + ExamplesLayout.GOLDEN_CMS_FIXTURE_DOC + " SHA-256 to: " + sha256);
  }

  private static String sha256Hex(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
