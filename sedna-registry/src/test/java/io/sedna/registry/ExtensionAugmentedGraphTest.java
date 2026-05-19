package io.sedna.registry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.RegistryVersion;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.VocabRef;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.validation.CompositeValidationEngine;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Forward/reverse validation path with registry bootstrapped from extension payload. */
class ExtensionAugmentedGraphTest {

  @Test
  void cmsGraphValidatesAgainstExtendedRegistry() {
    VocabRef extensionOnly = new VocabRef("ext", "DOMAIN.CUSTOM.TERM", "v1");
    SemanticDefinition definition =
        new SemanticDefinition(extensionOnly, "Custom", "Custom vocabulary entry");
    byte[] payload =
        TlvRegistryExtensionDecoder.encode(Map.of(extensionOnly.canonicalKey(), definition));

    InMemorySemanticRegistry registry = RegistryBootstrap.bootstrap(payload);
    assertTrue(registry.version().equals(new RegistryVersion("core", 1, 1)));

    var graph = CmsReferenceFixtureGraph.create();
    var validated = CompositeValidationEngine.standard(registry).validate(graph);
    assertTrue(validated.isOk(), () -> String.valueOf(validated.error()));
    assertTrue(validated.value().valid());
  }

  @Test
  void extendedRegistryResolvesCustomVocabulary() {
    VocabRef ref = new VocabRef("ext", "DOMAIN.CUSTOM.TERM", "v1");
    byte[] payload =
        TlvRegistryExtensionDecoder.encode(
            Map.of(ref.canonicalKey(), new SemanticDefinition(ref, "Custom", "Custom")));
    InMemorySemanticRegistry registry = RegistryBootstrap.bootstrap(payload);
    assertTrue(registry.resolve(ref).isOk());
  }
}
