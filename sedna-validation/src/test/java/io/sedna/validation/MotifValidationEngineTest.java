package io.sedna.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.Constraint;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.dna.SednaFoldV1;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import org.junit.jupiter.api.Test;

class MotifValidationEngineTest {

  private final MotifValidationEngine engine = new MotifValidationEngine();

  @Test
  void foldedGraphWithPartialMatchSurfacesFlag() {
    SemanticGraph graph = CmsReferenceFixtureGraph.create();
    var folded = SednaFoldMotifCodec.INSTANCE.fold(graph);
    assertTrue(folded.isOk());

    var report = engine.validate(folded.value());
    assertTrue(report.isOk());
    assertTrue(report.value().valid());
    // CMS reference is an exact CRUD match — no partial flag expected.
    assertEquals(0, report.value().flags().size());
  }

  @Test
  void motifWithoutPayloadFailsValidation() {
    SemanticGraph folded = SednaFoldMotifCodec.INSTANCE.fold(CmsReferenceFixtureGraph.create()).value();
    var motif = folded.nodes().getFirst();
    var invalidMotif =
        new io.sedna.core.GenomeNode(
            motif.nodeId(),
            motif.kind(),
            motif.core(),
            motif.contracts(),
            java.util.List.of(
                new Constraint(SednaFoldV1.MOTIF_REF_PREFIX + SednaFoldV1.MOTIF_CRUD_STACK)));
    SemanticGraph invalid =
        new SemanticGraph(java.util.List.of(invalidMotif), folded.links(), folded.vocabularyVersion());
    var report = engine.validate(invalid);
    assertTrue(!report.isOk());
  }
}
