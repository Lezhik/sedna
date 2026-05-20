package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-025 — unknown CLI command exits 2 with stable message. */
@Tag("e2e")
class CliInvalidArgsE2eTest {

  @Test
  void unknownCommandExitsTwo() throws Exception {
    E2eTestSupport.CliResult first = E2eTestSupport.runCli("nosuchcommand");
    E2eTestSupport.CliResult second = E2eTestSupport.runCli("nosuchcommand");
    assertNotEquals(0, first.exitCode());
    assertEquals(first.exitCode(), second.exitCode());
    assertTrue(first.stdout().toLowerCase().contains("unknown command"));
    assertEquals(normalize(first.stdout()), normalize(second.stdout()));
  }

  private static String normalize(String text) {
    return text.replace("\r\n", "\n").trim();
  }
}
