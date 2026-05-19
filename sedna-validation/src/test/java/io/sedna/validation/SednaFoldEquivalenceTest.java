package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.SemanticGraph;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class SednaFoldEquivalenceTest {

  @Test
  void expandFoldIsSemanticallyEquivalentToOriginal() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var folded = SednaFoldMotifCodec.INSTANCE.fold(graph);
    assertTrue(folded.isOk());
    var expanded = SednaFoldMotifCodec.INSTANCE.expand(folded.value());
    assertTrue(expanded.isOk());
    var equivalent = SemanticEquivalenceChecker.checkEquivalent(graph, expanded.value());
    assertTrue(equivalent.isOk(), () -> String.valueOf(equivalent.error()));
  }
}
