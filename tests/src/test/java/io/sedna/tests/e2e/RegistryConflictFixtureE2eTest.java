package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import io.sedna.registry.RegistryBootstrap;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-005 — registry collision payload on disk matches bootstrap API behavior. */
@Tag("e2e")
class RegistryConflictFixtureE2eTest {

  @BeforeAll
  static void materialize() throws Exception {
    E2eFixtures.materializeAllFixtures();
  }

  @Test
  void collisionFixtureBytesMatchApiAndReportIsStable() throws Exception {
    byte[] payload = Files.readAllBytes(E2eFixtures.registryCollisionPath());
    assertTrue(payload.length > 0);

    var first = RegistryBootstrap.bootstrapResult(payload);
    var second = RegistryBootstrap.bootstrapResult(payload);
    assertFalse(first.isOk());
    assertFalse(second.isOk());
    assertEquals(ErrorCode.VALIDATION_FAILED, first.error().code());
    assertEquals(first.error().message(), second.error().message());
    assertTrue(first.error().message().contains("collides"));
  }
}
