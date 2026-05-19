package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.RegistryVersion;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.registry.InMemorySemanticRegistry;
import org.junit.jupiter.api.Test;

class VocabularyVersionValidationEngineTest {

  @Test
  void acceptsCompatibleGraphVersion() {
    var registry = InMemorySemanticRegistry.bootstrap();
    var engine = new VocabularyVersionValidationEngine(registry);
    var result = engine.validate(CmsReferenceFixtureGraph.create());
    assertTrue(result.isOk());
    assertTrue(result.value().valid());
  }

  @Test
  void rejectsIncompatibleMinor() {
    var graph =
        new io.sedna.core.SemanticGraph(
            CmsReferenceFixtureGraph.create().nodes(),
            CmsReferenceFixtureGraph.create().links(),
            new RegistryVersion("core", 1, 99));
    var registry = InMemorySemanticRegistry.bootstrap();
    var engine = new VocabularyVersionValidationEngine(registry);
    var result = engine.validate(graph);
    assertFalse(result.isOk());
  }
}
