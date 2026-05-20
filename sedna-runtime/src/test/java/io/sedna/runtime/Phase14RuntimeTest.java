package io.sedna.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.persistence.FileCheckpointStore;
import io.sedna.runtime.bus.InMemoryTraceEventBus;
import io.sedna.runtime.distributed.LocalDistributedRuntimeCoordinator;
import io.sedna.runtime.monitor.RuntimeMonitoringServer;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Phase14RuntimeTest {

  @Test
  void distributedCoordinatorExecutesLocally() {
    var scheduler = new DefaultRuntimeScheduler();
    var plan = scheduler.build(CmsReferenceFixtureGraph.create(), io.sedna.core.ExecutionProfile.DAG);
    assertTrue(plan.isOk());
    var trace =
        new LocalDistributedRuntimeCoordinator()
            .execute(plan.value(), io.sedna.runtime.execution.RuntimeExecutionOptions.DEFAULT);
    assertTrue(trace.isOk());
    assertTrue(trace.value().events().size() >= 1);
  }

  @Test
  void inMemoryTraceBusOrdersEvents() {
    InMemoryTraceEventBus bus = new InMemoryTraceEventBus();
    var event =
        new io.sedna.runtime.trace.ExecutionTraceEvent(
            2L, 10L, new io.sedna.core.ExecutionToken(new byte[32]));
    assertTrue(bus.publish(event).isOk());
    assertEquals(1, bus.drainOrdered().value().size());
  }

  @Test
  void replayFromSpecificCheckpointSequence(@TempDir Path temp) {
    FileCheckpointStore store = new FileCheckpointStore(temp);
    RuntimeEngine engine = RuntimeServices.engine(store);
    var graph = CmsReferenceFixtureGraph.create();
    var trace = engine.run(graph);
    assertTrue(trace.isOk());

    var replay = RuntimeServices.replayHarness(store).replayFromCheckpoint(1L);
    assertTrue(replay.isOk(), () -> String.valueOf(replay.error()));
  }

  @Test
  void monitoringServerStarts() {
    var started = RuntimeMonitoringServer.start(0);
    assertTrue(started.isOk());
    try (RuntimeMonitoringServer server = started.value()) {
      assertTrue(server.port() > 0);
    }
  }
}
