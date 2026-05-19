package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.NodeKind;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class MutationSafetyEngineTest {

  @Test
  void detectsCrossDomainNodeRewrite() {
    SemanticGraph before = CmsReferenceFixtureGraph.create();
    long serviceId =
        before.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();

    var controller =
        before.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow();

    var tamperedNodes =
        before.nodes().stream()
            .map(
                node ->
                    node.nodeId() == controller.nodeId()
                        ? new io.sedna.core.GenomeNode(
                            node.nodeId(),
                            node.kind(),
                            node.core(),
                            node.contracts(),
                            java.util.List.of(new io.sedna.core.Constraint("READ_ONLY")))
                        : node)
            .toList();
    SemanticGraph after = new SemanticGraph(tamperedNodes, before.links(), before.vocabularyVersion());

    var result = new MutationSafetyEngine().verifySubtreeScope(before, after, serviceId);
    assertTrue(!result.isOk());
  }
}
