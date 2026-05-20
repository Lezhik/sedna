package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-013 — invalid graph rejection via CLI. */
@Tag("e2e")
class ValidateInvalidE2eTest {

  @BeforeAll
  static void writeFixture() throws Exception {
    E2eFixtures.ensureInvalidGraphFixtureOnDisk();
  }

  @Test
  void validateInvalidGraphExitsOneWithStableError() throws Exception {
    var fixture = E2eFixtures.invalidGraphFixturePath();
    E2eTestSupport.CliResult first =
        E2eTestSupport.runCli("validate", "--input=" + fixture, "--format=json");
    E2eTestSupport.CliResult second =
        E2eTestSupport.runCli("validate", "--input=" + fixture, "--format=json");
    assertEquals(1, first.exitCode());
    assertEquals(first.exitCode(), second.exitCode());
    assertTrue(first.stdout().contains(ErrorCode.VALIDATION_FAILED.name()));
    assertEquals(normalize(first.stdout()), normalize(second.stdout()));
  }

  private static String normalize(String text) {
    return text.replace("\r\n", "\n").trim();
  }
}
