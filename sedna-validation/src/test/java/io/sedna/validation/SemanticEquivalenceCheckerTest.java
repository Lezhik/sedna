package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class SemanticEquivalenceCheckerTest {

  @Test
  void identicalFixtureGraphsAreEquivalent() {
    var result =
        SemanticEquivalenceChecker.checkEquivalent(
            CmsReferenceFixtureGraph.create(), CmsReferenceFixtureGraph.create());
    assertTrue(result.isOk());
  }
}
