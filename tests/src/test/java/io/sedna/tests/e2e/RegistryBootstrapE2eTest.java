package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticCore;
import io.sedna.core.VocabRef;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-004 — registry bootstrap resolves fixture contracts. */
@Tag("e2e")
class RegistryBootstrapE2eTest {

  @Test
  void bootstrapResolvesAllFixtureContracts() throws Exception {
    var registry = InMemorySemanticRegistry.bootstrap();
    byte[] dna = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    var graph = DnaServices.decoder().decode(dna).value();
    for (VocabRef ref : vocabularyRefs(graph.nodes())) {
      assertTrue(registry.resolve(ref).isOk(), () -> "Unresolved: " + ref.canonicalKey());
    }
    for (VocabRef ref : vocabularyRefs(CmsReferenceFixtureGraph.create().nodes())) {
      assertTrue(registry.resolve(ref).isOk());
    }
  }

  private static Set<VocabRef> vocabularyRefs(java.util.List<GenomeNode> nodes) {
    Set<VocabRef> refs = new LinkedHashSet<>();
    for (GenomeNode node : nodes) {
      collectRefs(node.core(), refs);
    }
    return refs;
  }

  private static void collectRefs(SemanticCore core, Set<VocabRef> refs) {
    refs.add(core.classRef());
    refs.add(core.targetRef());
    refs.add(core.operationRef());
    refs.addAll(core.modifiers());
  }
}
