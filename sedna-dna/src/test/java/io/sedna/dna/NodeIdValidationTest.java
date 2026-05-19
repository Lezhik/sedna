package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.Result;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class NodeIdValidationTest {

  private final DnaEncoder encoder = DnaServices.encoder();
  private final DnaDecoder decoder = DnaServices.decoder();

  @Test
  void decodeRejectsMismatchedNodeId() {
    VocabRef ref = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
    SemanticCore core = new SemanticCore(ref, ref, ref, List.of());
    GenomeNode badNode = new GenomeNode(999L, NodeKind.ENTITY, core, List.of(), List.of());
    SemanticGraph graph =
        new SemanticGraph(
            List.of(badNode),
            List.of(),
            CmsReferenceFixtureGraph.create().vocabularyVersion());

    byte[] dna = encoder.encode(graph).value();
    Result<SemanticGraph, SemanticError> result = decoder.decode(dna);
    assertFalse(result.isOk());
    assertEquals(ErrorCode.INVALID_DNA, result.error().code());
  }
}
