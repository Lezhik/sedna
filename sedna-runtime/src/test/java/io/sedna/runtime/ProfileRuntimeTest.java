package io.sedna.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.NodeKind;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.trace.TraceEventKind;
import org.junit.jupiter.api.Test;

class ProfileRuntimeTest {

  @Test
  void schedulerAcceptsStatefulProfile() {
    var scheduler = new DefaultRuntimeScheduler();
    var graph = CmsReferenceFixtureGraph.create();
    var plan = scheduler.build(graph, ExecutionProfile.STATEFUL);
    assertTrue(plan.isOk(), () -> String.valueOf(plan.error()));
    assertEquals(ExecutionProfile.STATEFUL, plan.value().profile());
  }

  @Test
  void statefulExecutionRecordsFsmInCheckpoints() {
    InMemoryCheckpointStore store = new InMemoryCheckpointStore();
    RuntimeEngine engine = RuntimeServices.engine(store);
    var graph = CmsReferenceFixtureGraph.create();

    var trace = engine.run(graph, ExecutionProfile.STATEFUL, RuntimeExecutionOptions.DEFAULT);
    assertTrue(trace.isOk(), () -> String.valueOf(trace.error()));
    assertTrue(
        trace.value().events().stream().anyMatch(event -> event.kind() == TraceEventKind.STATE_TRANSITION));

    var checkpoint = store.findBySequence(2L);
    assertTrue(checkpoint.isOk());
    assertFalse(checkpoint.value().fsmState().isBlank());
    assertTrue(checkpoint.value().completedNodes() >= 1);
  }

  @Test
  void supervisorCompensatesPriorNodesInReverseOrder() {
    RuntimeEngine engine = RuntimeServices.engine();
    var graph = CmsReferenceFixtureGraph.create();
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();

    var trace =
        engine.run(
            graph,
            ExecutionProfile.SUPERVISOR,
            RuntimeExecutionOptions.injectFailure(controllerId));
    assertTrue(trace.isOk(), () -> String.valueOf(trace.error()));

    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();
    long entityId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.ENTITY)
            .findFirst()
            .orElseThrow()
            .nodeId();

    var compensation =
        trace.value().events().stream().filter(event -> event.kind() == TraceEventKind.COMPENSATE).toList();
    assertEquals(2, compensation.size());
    assertEquals(serviceId, compensation.get(0).nodeId());
    assertEquals(entityId, compensation.get(1).nodeId());
  }

  @Test
  void rejectsSupervisorOnSingleNodeGraph() {
    var scheduler = new DefaultRuntimeScheduler();
    var graph = CmsReferenceFixtureGraph.create();
    var folded = io.sedna.dna.SednaFoldMotifCodec.INSTANCE.fold(graph);
    assertTrue(folded.isOk());
    var plan = scheduler.build(folded.value(), ExecutionProfile.SUPERVISOR);
    assertTrue(!plan.isOk());
    assertEquals(io.sedna.core.ErrorCode.UNSUPPORTED_PROFILE, plan.error().code());
  }
}
