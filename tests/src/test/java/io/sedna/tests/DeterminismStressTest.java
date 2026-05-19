package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.sedna.core.CanonicalOrdering;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Parallel encode/decode stress (same inputs must yield identical bytes). */
@Execution(ExecutionMode.CONCURRENT)
class DeterminismStressTest {

  @Test
  void parallelEncodesAreByteIdentical() throws Exception {
    var graph = CmsReferenceFixtureGraph.create();
    byte[] baseline = DnaServices.encoder().encode(graph).value();

    try (var executor = Executors.newFixedThreadPool(8)) {
      List<Callable<byte[]>> tasks = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        tasks.add(() -> DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());
      }
      for (Future<byte[]> future : executor.invokeAll(tasks)) {
        assertArrayEquals(baseline, future.get());
      }
    }
  }

  @Test
  void repeatedForkedDecodesMatchGraph() {
    byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    var expected = CmsReferenceFixtureGraph.create();
    for (int fork = 0; fork < 8; fork++) {
      assertEquals(
          CanonicalOrdering.canonicalize(expected),
          CanonicalOrdering.canonicalize(DnaServices.decoder().decode(dna).value()),
          "fork " + fork);
    }
  }
}
