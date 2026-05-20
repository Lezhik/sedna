package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.training.model.SemanticEmbedding;
import io.sedna.training.model.SemanticTrajectory;
import io.sedna.training.model.TrainingDataset;
import io.sedna.training.model.TrainingProjectResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticEmbeddingIndexTest {

  @Test
  void retrievalIsDeterministicAndRanksIdenticalVectorsFirst() {
    var graph = CmsReferenceFixtureGraph.create();
    var embedder = new DeterministicSemanticEmbedder();
    var embeddings = embedder.embed(graph);
    SemanticEmbedding query = embeddings.getFirst();

    TrainingProjectResult near =
        new TrainingProjectResult(
            "/near",
            new SemanticTrajectory("/near", List.of(), List.of(), List.of()),
            embeddings,
            List.of(),
            List.of());
    TrainingProjectResult far =
        new TrainingProjectResult(
            "/far",
            new SemanticTrajectory("/far", List.of(), List.of(), List.of()),
            List.of(new SemanticEmbedding(999L, "other>path>op", "00".repeat(32))),
            List.of(),
            List.of());

    SemanticEmbeddingIndex index =
        SemanticEmbeddingIndex.fromDataset(new TrainingDataset(List.of(near, far), "fp"));

    var first = index.nearestNeighbors("/query", query.nodeId(), query.embeddingHex(), 1);
    var second = index.nearestNeighbors("/query", query.nodeId(), query.embeddingHex(), 1);

    assertEquals(first, second);
    assertEquals(1, first.size());
    assertEquals("/near", first.getFirst().projectPath());
    assertTrue(first.getFirst().similarity() > 0.99);
  }

  @Test
  void fingerprintStableAcrossRebuilds() {
    var graph = CmsReferenceFixtureGraph.create();
    var embeddings = new DeterministicSemanticEmbedder().embed(graph);
    TrainingProjectResult project =
        new TrainingProjectResult(
            "/p",
            new SemanticTrajectory("/p", List.of(), List.of(), List.of()),
            embeddings,
            List.of(),
            List.of());
    TrainingDataset dataset = new TrainingDataset(List.of(project), "abc");
    String first = SemanticEmbeddingIndex.fromDataset(dataset).fingerprint();
    String second = SemanticEmbeddingIndex.fromDataset(dataset).fingerprint();
    assertEquals(first, second);
    assertFalse(first.isBlank());
  }
}
