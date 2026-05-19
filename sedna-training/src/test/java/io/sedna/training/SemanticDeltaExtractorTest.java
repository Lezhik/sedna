package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticDeltaExtractorTest {

  @Test
  void detectsConstraintInjection() {
    var graph = CmsReferenceFixtureGraph.create();
    var service =
        graph.nodes().stream().filter(node -> node.kind() == NodeKind.SERVICE).findFirst().orElseThrow();
    long serviceId = service.nodeId();
    var updated =
        new GenomeNode(
            service.nodeId(),
            service.kind(),
            service.core(),
            service.contracts(),
            List.of(new Constraint("STATELESS_ONLY")));
    var nodes =
        graph.nodes().stream()
            .map(node -> node.nodeId() == serviceId ? updated : node)
            .toList();
    var after = new io.sedna.core.SemanticGraph(nodes, graph.links(), graph.vocabularyVersion());

    var deltas = new SemanticDeltaExtractor().extract("abc123", graph, after);
    assertTrue(deltas.stream().anyMatch(delta -> delta.deltaKind().equals("CONSTRAINT_CHANGED")));
    assertEquals(1, deltas.size());
  }
}
