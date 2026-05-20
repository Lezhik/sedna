package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.training.RegistryUpdateProposer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-023 — registry learning proposals are canonically ordered. */
@Tag("e2e")
class RegistryLearningE2eTest {

  @Test
  void proposalListIsStable() {
    var graph = CmsReferenceFixtureGraph.create();
    var proposer = new RegistryUpdateProposer(InMemorySemanticRegistry.bootstrap());
    assertEquals(proposer.propose(graph), proposer.propose(graph));
  }
}
