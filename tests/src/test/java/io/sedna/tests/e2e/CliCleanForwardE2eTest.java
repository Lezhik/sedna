package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E — CLI {@code --clean} removes prior output before forward. */
@Tag("e2e")
class CliCleanForwardE2eTest {

  @Test
  void forwardCleanRemovesStaleFile() throws Exception {
    Path out = E2eTestSupport.outputDir("E2E-clean-forward").resolve("generated");
    E2eTestSupport.prepareDir(out.getParent());
    Files.createDirectories(out);
    Path stale = out.resolve("stale.txt");
    Files.writeString(stale, "stale");

    Path fixture = E2eTestSupport.readGoldenFixture();
    E2eTestSupport.CliResult result =
        E2eTestSupport.runCli(
            "forward", "--input=" + fixture, "--output=" + out, "--clean");
    assertEquals(0, result.exitCode(), () -> result.stdout());
    assertFalse(Files.exists(stale));
    assertTrue(Files.isRegularFile(out.resolve("build.gradle.kts")));
  }
}
