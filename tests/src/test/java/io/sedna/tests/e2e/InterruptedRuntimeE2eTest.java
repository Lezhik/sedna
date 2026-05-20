package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.NodeKind;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.runtime.RuntimeServices;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-030 — interrupted execution resumes with stable trace hash. */
@Tag("e2e")
class InterruptedRuntimeE2eTest {

  @Test
  void supervisorFailureThenReplayMatches() {
    var graph = CmsReferenceFixtureGraph.create();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();

    InMemoryCheckpointStore store = new InMemoryCheckpointStore();
    var engine = RuntimeServices.engine(store);
    var trace =
        engine.run(
            graph,
            ExecutionProfile.SUPERVISOR,
            RuntimeExecutionOptions.injectFailure(controllerId));
    assertTrue(trace.isOk());

    var replay = RuntimeServices.replayHarness(store).verifyReplayMatches(trace.value());
    assertTrue(replay.isOk(), () -> String.valueOf(replay.error()));
  }
}
