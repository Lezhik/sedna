package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.CanonicalOrdering;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.mutation.MutationServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-019 — cross-domain mutation rolls back (invalid-cross-domain.json intent). */
@Tag("e2e")
class MutationRejectE2eTest {

  @Test
  void crossDomainSubtreeReplaceRollsBack() {
    var graph = CmsReferenceFixtureGraph.create();
    var engine = MutationServices.engine();
    var result = engine.apply(graph, E2eFixtures.invalidCrossDomainMutation(graph));
    assertTrue(result.isOk());
    assertTrue(result.value().rolledBack());
    assertEquals(CanonicalOrdering.canonicalize(graph), result.value().graph());
  }
}
