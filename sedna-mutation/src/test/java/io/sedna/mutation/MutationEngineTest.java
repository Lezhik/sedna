package io.sedna.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.core.NodeKind;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.dna.NodeIdHasher;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MutationEngineTest {

  private final DefaultMutationEngine engine = (DefaultMutationEngine) MutationServices.engine();

  @Test
  void constraintInjectionCommits() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();

    Mutation mutation =
        new Mutation(
            serviceId,
            MutationType.CONSTRAINT_INJECTION,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(new Constraint("STATELESS_ONLY")));

    var result = engine.apply(graph, mutation);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));
    assertFalse(result.value().rolledBack());
    assertTrue(
        result.value().graph().nodes().stream()
            .filter(node -> node.nodeId() == serviceId)
            .anyMatch(node -> node.constraints().stream().anyMatch(c -> c.code().equals("STATELESS_ONLY"))));
  }

  @Test
  void deleteRootSubtreeRollsBack() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();

    var result = engine.apply(graph, new Mutation(controllerId, MutationType.NODE_DELETE));
    assertTrue(result.isOk());
    assertTrue(result.value().rolledBack());
    assertEquals(CanonicalOrdering.canonicalize(graph), result.value().graph());
  }

  @Test
  void insertIntegrationNodeCommits() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();

    VocabRef integration = new VocabRef("spring", "SERVICE.STATELESS", "v1");
    SemanticCore core = new SemanticCore(integration, integration, integration, List.of());
    GenomeNode integrationNode =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(0L, NodeKind.INTEGRATION, core, List.of(), List.of()));

    Mutation mutation =
        new Mutation(
            serviceId,
            MutationType.NODE_INSERT,
            Optional.of(integrationNode),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var result = engine.apply(graph, mutation);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));
    assertFalse(result.value().rolledBack());
    assertEquals(graph.nodes().size() + 1, result.value().graph().nodes().size());
  }

  @Test
  void deterministicMutationOrderingIsStable() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();

    List<Mutation> batch =
        List.of(
            new Mutation(
                controllerId,
                MutationType.CONSTRAINT_INJECTION,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new Constraint("READ_ONLY"))),
            new Mutation(
                serviceId,
                MutationType.CONSTRAINT_INJECTION,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new Constraint("STATELESS_ONLY"))));

    SemanticGraph first = graph;
    SemanticGraph second = graph;
    var ordering =
        batch.stream()
            .sorted(
                java.util.Comparator.comparingLong(Mutation::targetNodeId)
                    .thenComparing(mutation -> mutation.operation().name()))
            .toList();
    for (Mutation mutation : ordering) {
      var left = engine.apply(first, mutation);
      var right = engine.apply(second, mutation);
      assertFalse(left.value().rolledBack());
      assertFalse(right.value().rolledBack());
      first = left.value().graph();
      second = right.value().graph();
    }
    assertEquals(first, second);
  }
}
