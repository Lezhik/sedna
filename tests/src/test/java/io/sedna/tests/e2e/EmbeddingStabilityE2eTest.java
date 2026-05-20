package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.training.DeterministicSemanticEmbedder;
import io.sedna.training.model.SemanticEmbedding;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-022 — embedding stability (cosine ≥ 0.9999, no byte hash on floats). */
@Tag("e2e")
class EmbeddingStabilityE2eTest {

  private static final double COSINE_MIN = 0.9999;
  private static final double EPSILON = 1e-6;

  @Test
  void embeddingsAreStableAcrossThreeRuns() {
    var graph = CmsReferenceFixtureGraph.create();
    var embedder = new DeterministicSemanticEmbedder();
    List<SemanticEmbedding> run1 = embedder.embed(graph);
    List<SemanticEmbedding> run2 = embedder.embed(graph);
    List<SemanticEmbedding> run3 = embedder.embed(graph);
    assertEquals(run1, run2);
    assertEquals(run2, run3);

    for (int i = 0; i < run1.size(); i++) {
      double cosine = cosineSimilarity(run1.get(i).embeddingHex(), run2.get(i).embeddingHex());
      assertTrue(cosine >= COSINE_MIN, () -> "cosine=" + cosine);
      assertTrue(maxAbsDelta(run1.get(i).embeddingHex(), run2.get(i).embeddingHex()) <= EPSILON);
    }
  }

  private static double cosineSimilarity(String leftHex, String rightHex) {
    double[] left = toUnitVector(leftHex);
    double[] right = toUnitVector(rightHex);
    double sum = 0.0;
    for (int i = 0; i < left.length; i++) {
      sum += left[i] * right[i];
    }
    return sum;
  }

  private static double maxAbsDelta(String leftHex, String rightHex) {
    byte[] left = HexFormat.of().parseHex(leftHex);
    byte[] right = HexFormat.of().parseHex(rightHex);
    double max = 0.0;
    for (int i = 0; i < left.length; i++) {
      max = Math.max(max, Math.abs(Byte.toUnsignedInt(left[i]) - Byte.toUnsignedInt(right[i])));
    }
    return max;
  }

  private static double[] toUnitVector(String embeddingHex) {
    byte[] bytes = HexFormat.of().parseHex(embeddingHex);
    double[] vector = new double[bytes.length];
    double norm = 0.0;
    for (int i = 0; i < bytes.length; i++) {
      vector[i] = Byte.toUnsignedInt(bytes[i]);
      norm += vector[i] * vector[i];
    }
    norm = Math.sqrt(norm);
    if (norm == 0.0) {
      return vector;
    }
    for (int i = 0; i < vector.length; i++) {
      vector[i] /= norm;
    }
    return vector;
  }
}
