package io.sedna.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.RegistryVersion;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.VocabRef;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TlvRegistryExtensionDecoderTest {

  @Test
  void roundTripExtensionPayload() {
    VocabRef ref = new VocabRef("ext", "DOMAIN.CUSTOM.TERM", "v1");
    SemanticDefinition definition = new SemanticDefinition(ref, "Custom Term", "Custom Term");
    byte[] payload = TlvRegistryExtensionDecoder.encode(Map.of(ref.canonicalKey(), definition));

    var decoded = new TlvRegistryExtensionDecoder().decode(payload);
    assertTrue(decoded.isOk());
    assertEquals(1, decoded.value().size());
    assertEquals("Custom Term", decoded.value().get(ref.canonicalKey()).displayName());
  }

  @Test
  void bootstrapBumpsMinorWhenExtensionsPresent() {
    VocabRef ref = new VocabRef("ext", "DOMAIN.CUSTOM.TERM", "v1");
    SemanticDefinition definition = new SemanticDefinition(ref, "Custom Term", "Custom Term");
    byte[] payload = TlvRegistryExtensionDecoder.encode(Map.of(ref.canonicalKey(), definition));

    InMemorySemanticRegistry registry = RegistryBootstrap.bootstrap(payload);
    assertEquals(new RegistryVersion("core", 1, 1), registry.version());
    assertTrue(registry.resolve(ref).isOk());
  }
}
