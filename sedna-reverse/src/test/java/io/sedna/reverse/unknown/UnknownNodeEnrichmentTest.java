package io.sedna.reverse.unknown;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.NodeKind;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.ParsedProject;
import io.sedna.reverse.model.StructuralGraph;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnknownNodeEnrichmentTest {

  @Test
  void addsUnknownNodesWithoutChangingLinks() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    int linkCount = graph.links().size();

    Map<String, ParsedClass> classes = new LinkedHashMap<>();
    for (var node : graph.nodes()) {
      for (var contract : node.contracts()) {
        String payload = contract.ioSchema().payload();
        if (payload.startsWith("class:")) {
          String qualified = payload.substring("class:".length());
          classes.put(qualified, parsed(qualified));
        }
      }
    }
    classes.put("com.example.ExtraHelper", parsed("com.example.ExtraHelper"));

    StructuralGraph structural =
        new StructuralGraph(new ParsedProject(Path.of("."), classes), List.of());

    var enriched =
        new UnknownNodeEnrichmentStep()
            .enrich(structural, graph, DisabledUnknownLabelProvider.INSTANCE);
    assertTrue(enriched.isOk());
    assertTrue(
        enriched.value().nodes().stream().anyMatch(node -> node.kind() == NodeKind.INTEGRATION));
    assertEquals(linkCount, enriched.value().links().size());
  }

  private static void assertEquals(int expected, int actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private static ParsedClass parsed(String qualified) {
  int dot = qualified.lastIndexOf('.');
    String pkg = dot >= 0 ? qualified.substring(0, dot) : "";
    String simple = dot >= 0 ? qualified.substring(dot + 1) : qualified;
    return new ParsedClass(qualified, pkg, simple, List.of(), List.of(), List.of());
  }
}
