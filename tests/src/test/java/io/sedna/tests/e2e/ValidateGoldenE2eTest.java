package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-012 — CLI validate on golden fixture. */
@Tag("e2e")
class ValidateGoldenE2eTest {

  @Test
  void validateGoldenFixtureExitsZero() throws Exception {
    Path fixture = E2eTestSupport.readGoldenFixture();
    E2eTestSupport.CliResult result =
        E2eTestSupport.runCli(
            "validate", "--input=" + fixture, "--format=json");
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("\"status\":\"ok\""));
  }
}
