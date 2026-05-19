package io.sedna.reverse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class ReverseCmsReferenceTest {

  private static final Path CMS_REFERENCE =
      Paths.get("..", "examples", "cms-reference").normalize().toAbsolutePath();

  @Test
  void reverseCmsReferenceMatchesFixtureGraph() {
    var pipeline = ReverseServices.pipeline();
    var graph = pipeline.reverseGraph(CMS_REFERENCE);
    assertTrue(graph.isOk(), () -> String.valueOf(graph.error()));

    var expected = CmsReferenceFixtureGraph.create();
    var equivalent = SemanticEquivalenceChecker.checkEquivalent(expected, graph.value());
    assertTrue(equivalent.isOk(), () -> String.valueOf(equivalent.error()));
  }

  @Test
  void reverseCmsReferenceProducesDeterministicDnaBytes() {
    byte[] golden = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    var pipeline = ReverseServices.pipeline();

    byte[] first = pipeline.reverse(CMS_REFERENCE).value();
    byte[] second = pipeline.reverse(CMS_REFERENCE).value();

    assertArrayEquals(golden, first);
    assertArrayEquals(first, second);
  }
}
