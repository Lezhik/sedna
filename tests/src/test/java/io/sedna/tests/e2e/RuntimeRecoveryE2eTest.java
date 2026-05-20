package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.NodeKind;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.runtime.RuntimeServices;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.trace.TraceHasher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-016 — checkpoint recovery after injected failure. */
@Tag("e2e")
class RuntimeRecoveryE2eTest {

  @Test
  void restoreFromCheckpointMatchesFullTrace() {
    var graph = CmsReferenceFixtureGraph.create();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();

    InMemoryCheckpointStore store = new InMemoryCheckpointStore();
    var engine = RuntimeServices.engine(store);
    var full = engine.run(graph);
    assertTrue(full.isOk());
    String fullHash = TraceHasher.sha256(full.value());

    var withFailure =
        engine.run(graph, io.sedna.core.ExecutionProfile.DAG, RuntimeExecutionOptions.injectFailure(controllerId));
    assertTrue(withFailure.isOk());

    var restored = engine.restoreAndContinue(1L);
    assertTrue(restored.isOk(), () -> String.valueOf(restored.error()));
    assertEquals(fullHash, TraceHasher.sha256(restored.value()));
  }
}
