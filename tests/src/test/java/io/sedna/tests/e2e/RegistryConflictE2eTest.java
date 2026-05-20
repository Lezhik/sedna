package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import io.sedna.registry.RegistryBootstrap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-005 — registry extension collision is deterministic. */
@Tag("e2e")
class RegistryConflictE2eTest {

  @Test
  void collisionReportIsStableAcrossRuns() {
    byte[] payload = E2eFixtures.registryCollisionExtensionPayload();
    var first = RegistryBootstrap.bootstrapResult(payload);
    var second = RegistryBootstrap.bootstrapResult(payload);
    assertFalse(first.isOk());
    assertFalse(second.isOk());
    assertEquals(ErrorCode.VALIDATION_FAILED, first.error().code());
    assertEquals(first.error().code(), second.error().code());
    assertEquals(first.error().message(), second.error().message());
    assertTrue(first.error().message().contains("collides"));
  }
}
