package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.registry.RegistryBootstrap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-029 — corrupted registry extension falls back to clean bootstrap. */
@Tag("e2e")
class RegistryRecoveryE2eTest {

  @Test
  void corruptedExtensionFailsThenCoreBootstrapWorks() {
    byte[] corrupt = E2eFixtures.corruptedRegistryExtensionPayload();
    var failed = RegistryBootstrap.bootstrapResult(corrupt);
    assertFalse(failed.isOk());

    var recovered = InMemorySemanticRegistry.bootstrap();
    assertTrue(recovered.resolve(
            new io.sedna.core.VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1"))
        .isOk());
  }
}
