package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.mutation.MutationServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.validation.CompositeValidationEngine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-017 — subtree mutation applies and validates (see tests/fixtures/mutations/). */
@Tag("e2e")
class MutationApplyE2eTest {

  @Test
  void addPaymentModuleMutationCommits() {
    var graph = CmsReferenceFixtureGraph.create();
    var engine = MutationServices.engine();
    var result = engine.apply(graph, E2eFixtures.addPaymentModuleMutation(graph));
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));
    assertFalse(result.value().rolledBack());

    var registry = InMemorySemanticRegistry.bootstrap();
    var validated = CompositeValidationEngine.standard(registry).validate(result.value().graph());
    assertTrue(validated.isOk());
    assertTrue(validated.value().valid());
  }
}
