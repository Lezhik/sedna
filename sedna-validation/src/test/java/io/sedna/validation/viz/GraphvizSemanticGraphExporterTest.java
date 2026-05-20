package io.sedna.validation.viz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class GraphvizSemanticGraphExporterTest {

  @Test
  void exportsDeterministicDot() {
    var graph = CmsReferenceFixtureGraph.create();
    String first = GraphvizSemanticGraphExporter.toDot(graph, "cms");
    String second = GraphvizSemanticGraphExporter.toDot(graph, "cms");
    assertTrue(first.contains("digraph cms"));
    assertTrue(first.contains("->"));
    assertTrue(first.equals(second));
  }
}
