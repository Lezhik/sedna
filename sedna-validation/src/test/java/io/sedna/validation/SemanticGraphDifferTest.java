package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class SemanticGraphDifferTest {

  @Test
  void identicalGraphsHaveNoDeltas() {
    var graph = CmsReferenceFixtureGraph.create();
    var diffs = new SemanticGraphDiffer().diff(graph, graph);
    assertTrue(diffs.isEmpty());
  }

  @Test
  void detectsConstraintChange() {
    var graph = CmsReferenceFixtureGraph.create();
    var service =
        graph.nodes().stream().filter(node -> node.kind() == io.sedna.core.NodeKind.SERVICE).findFirst().orElseThrow();
    var updated =
        new io.sedna.core.GenomeNode(
            service.nodeId(),
            service.kind(),
            service.core(),
            service.contracts(),
            java.util.List.of(new io.sedna.core.Constraint("STATELESS_ONLY")));
    var nodes =
        graph.nodes().stream().map(node -> node.nodeId() == service.nodeId() ? updated : node).toList();
    var after = new io.sedna.core.SemanticGraph(nodes, graph.links(), graph.vocabularyVersion());
    var diffs = new SemanticGraphDiffer().diff(graph, after);
    assertFalse(diffs.isEmpty());
    assertTrue(diffs.stream().anyMatch(entry -> entry.kind().equals("CONSTRAINT_CHANGED")));
  }
}
