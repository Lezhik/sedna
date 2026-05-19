package io.sedna.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ExecutionProfile;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.runtime.trace.TraceHasher;
import org.junit.jupiter.api.Test;

class RuntimeReplayTest {

  @Test
  void replayProducesIdenticalTraceHash() {
    InMemoryCheckpointStore store = new InMemoryCheckpointStore();
    RuntimeEngine engine = RuntimeServices.engine(store);
    var graph = CmsReferenceFixtureGraph.create();

    var trace = engine.run(graph);
    assertTrue(trace.isOk(), () -> String.valueOf(trace.error()));

    var replay = RuntimeServices.replayHarness(store).verifyReplayMatches(trace.value());
    assertTrue(replay.isOk(), () -> String.valueOf(replay.error()));
  }

  @Test
  void checkpointRestoreResumesSameOrder() {
    InMemoryCheckpointStore store = new InMemoryCheckpointStore();
    RuntimeEngine engine = RuntimeServices.engine(store);
    var graph = CmsReferenceFixtureGraph.create();

    var first = engine.run(graph);
    assertTrue(first.isOk());
    String firstHash = TraceHasher.sha256(first.value());

    var restored = engine.restoreAndContinue(1L);
    assertTrue(restored.isOk(), () -> String.valueOf(restored.error()));
    assertEquals(firstHash, TraceHasher.sha256(restored.value()));
  }

  @Test
  void rejectsNonDagProfile() {
    var scheduler = new DefaultRuntimeScheduler();
    var graph = CmsReferenceFixtureGraph.create();
    var result = scheduler.build(graph, ExecutionProfile.STATEFUL);
    assertTrue(!result.isOk());
    assertEquals(io.sedna.core.ErrorCode.UNSUPPORTED_PROFILE, result.error().code());
  }
}
