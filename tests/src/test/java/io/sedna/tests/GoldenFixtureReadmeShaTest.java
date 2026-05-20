package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Enforces golden SHA-256 documented in examples/docs/cms-reference-fixture.md. */
class GoldenFixtureReadmeShaTest {

  private static final Pattern SHA_LINE =
      Pattern.compile("^[0-9a-f]{64}$", Pattern.MULTILINE);

  @Test
  void fixtureBytesMatchReadmeSha256() throws Exception {
    Path repoRoot = RepoPaths.locateRoot();
    Path fixture = io.sedna.core.examples.ExamplesLayout.goldenCmsFixture(repoRoot);
    Path readme = io.sedna.core.examples.ExamplesLayout.goldenCmsFixtureDoc(repoRoot);
    assertTrue(Files.isRegularFile(fixture), "Missing " + fixture);
    assertTrue(Files.isRegularFile(readme), "Missing " + readme);

    String documentedSha = parseShaFromReadme(Files.readString(readme));
    byte[] bytes = Files.readAllBytes(fixture);
    assertEquals(documentedSha, sha256(bytes), "Fixture bytes drifted; update README and commit");
  }

  private static String parseShaFromReadme(String readme) {
    Matcher matcher = SHA_LINE.matcher(readme);
    if (!matcher.find()) {
      throw new IllegalStateException("No 64-char SHA-256 hex found in fixture README");
    }
    return matcher.group();
  }

  private static String sha256(byte[] bytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(bytes);
    return HexFormat.of().formatHex(digest.digest());
  }
}
