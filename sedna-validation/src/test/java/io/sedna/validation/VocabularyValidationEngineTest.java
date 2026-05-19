package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.RegistryVersion;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.registry.InMemorySemanticRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class VocabularyValidationEngineTest {

  @Test
  void resolvesEmbeddedVocabulary() {
    var engine = new VocabularyValidationEngine(InMemorySemanticRegistry.bootstrap());
    assertTrue(engine.validate(CmsReferenceFixtureGraph.create()).isOk());
  }

  @Test
  void unknownVocabFails() {
    var engine = new VocabularyValidationEngine(InMemorySemanticRegistry.bootstrap());
    VocabRef unknown = new VocabRef("core", "UNKNOWN.TERM", "v1");
    SemanticCore core = new SemanticCore(unknown, unknown, unknown, List.of());
    GenomeNode node = new GenomeNode(1L, NodeKind.ENTITY, core, List.of(), List.of());
    SemanticGraph graph =
        new SemanticGraph(List.of(node), List.of(), new RegistryVersion("core", 1, 0));
    var result = engine.validate(graph);
    assertFalse(result.isOk());
    assertTrue(result.error().code() == ErrorCode.UNKNOWN_VOCAB);
  }
}
