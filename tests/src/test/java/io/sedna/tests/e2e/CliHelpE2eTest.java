package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-024 — CLI help lists core commands. */
@Tag("e2e")
class CliHelpE2eTest {

  @Test
  void helpListsPipelineCommands() throws Exception {
    E2eTestSupport.CliResult result = E2eTestSupport.runCli("help");
    assertEquals(0, result.exitCode());
    String output = result.stdout();
    assertTrue(output.contains("forward"));
    assertTrue(output.contains("reverse"));
    assertTrue(output.contains("validate"));
  }
}
