package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.Constraint;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.mutation.MutationServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.validation.CompositeValidationEngine;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-019B — ten valid mutations keep graph valid and forward-compilable. */
@Tag("e2e")
class DeepMutationDriftE2eTest {

  @Test
  void tenMutationsRemainValidAndCompile() throws Exception {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var engine = MutationServices.engine();
    var registry = InMemorySemanticRegistry.bootstrap();
    var validation = CompositeValidationEngine.standard(registry);

    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == io.sedna.core.NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == io.sedna.core.NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();
    List<String> labels =
        List.of(
            "STATELESS_ONLY",
            "READ_ONLY",
            "STATELESS_ONLY",
            "READ_ONLY",
            "STATELESS_ONLY",
            "READ_ONLY",
            "STATELESS_ONLY",
            "READ_ONLY",
            "STATELESS_ONLY",
            "READ_ONLY");
    List<Mutation> mutations =
        java.util.stream.IntStream.range(0, labels.size())
            .mapToObj(
                i ->
                    new Mutation(
                        i % 2 == 0 ? serviceId : controllerId,
                        MutationType.CONSTRAINT_INJECTION,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new Constraint(labels.get(i)))))
            .toList();

    SemanticGraph current = graph;
    for (Mutation mutation : mutations) {
      var applied = engine.apply(current, mutation);
      assertTrue(applied.isOk(), () -> String.valueOf(applied.error()));
      assertFalse(applied.value().rolledBack());
      current = applied.value().graph();
      var report = validation.validate(current);
      assertTrue(report.isOk() && report.value().valid());
    }

    byte[] dna = DnaServices.encoder().encode(current).value();
    Path generated = E2eTestSupport.outputDir("E2E-019B").resolve("generated");
    E2eTestSupport.prepareDir(generated.getParent());
    E2eTestSupport.prepareDir(generated);
    var forward =
        ForwardServices.pipeline(registry, DisabledLlmClient.INSTANCE);
    var written = forward.runToDirectory(dna, generated);
    assertTrue(written.isOk(), () -> String.valueOf(written.error()));
    E2eTestSupport.runGradleBuild(E2eTestSupport.repoRoot(), generated);
  }
}
