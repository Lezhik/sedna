package io.sedna.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Smoke latency gate for runtime plan build (reference graph, warmed JVM). */
class RuntimeSchedulingLatencyTest {

  @Test
  void buildPlanCompletesWithinBudget() {
    DefaultRuntimeScheduler scheduler = new DefaultRuntimeScheduler();
    var graph = CmsReferenceFixtureGraph.create();
    Instant start = Instant.now();
    for (int i = 0; i < 100; i++) {
      var plan = scheduler.build(graph);
      assertTrue(plan.isOk());
    }
    Duration elapsed = Duration.between(start, Instant.now());
    long perOpMs = elapsed.toMillis() / 100;
    assertTrue(
        perOpMs < 50,
        "Average scheduling time " + perOpMs + "ms exceeds 50ms budget (100 iterations)");
  }
}
