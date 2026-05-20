package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.core.NodeKind;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.mutation.MutationServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-018 — invalid subtree mutation rolls back to baseline. */
@Tag("e2e")
class MutationRollbackE2eTest {

  @Test
  void deleteControllerRollsBack() {
    var graph = CmsReferenceFixtureGraph.create();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();
    var engine = MutationServices.engine();
    var result = engine.apply(graph, new Mutation(controllerId, MutationType.NODE_DELETE));
    assertTrue(result.isOk());
    assertTrue(result.value().rolledBack());
    assertEquals(CanonicalOrdering.canonicalize(graph), result.value().graph());
  }
}
