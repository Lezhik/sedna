package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.training.MutationDatasetGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-021 — deterministic motif/mutation candidate discovery. */
@Tag("e2e")
class MotifDiscoveryE2eTest {

  @Test
  void mutationCandidatesAreStableAcrossRuns() {
    var graph = CmsReferenceFixtureGraph.create();
    var generator = new MutationDatasetGenerator();
    assertEquals(generator.generate(graph), generator.generate(graph));
    assertFalse(generator.generate(graph).isEmpty());
  }
}
