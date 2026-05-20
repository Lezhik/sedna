package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.training.model.SemanticSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class MutationDatasetFromHistoryTest {

  @Test
  void prefixesRowsWithCommitHash() {
    var graph = CmsReferenceFixtureGraph.create();
    var snapshots =
        List.of(
            new SemanticSnapshot("abc111", graph, "fp1"),
            new SemanticSnapshot("def222", graph, "fp2"));
    var rows = new MutationDatasetFromHistory().generate(snapshots);
    assertTrue(rows.size() >= 2);
    assertTrue(rows.stream().anyMatch(row -> row.label().startsWith("abc111:")));
    assertTrue(rows.stream().anyMatch(row -> row.label().startsWith("def222:")));
    assertEquals(rows.size(), rows.stream().map(row -> row.label()).distinct().count());
  }
}
