package io.sedna.training;

import io.sedna.training.model.EmbeddingNeighbor;
import io.sedna.training.model.SemanticEmbedding;
import io.sedna.training.model.TrainingDataset;
import io.sedna.training.model.TrainingProjectResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Pure Java approximate nearest-neighbor retrieval over SEDNA-EMBED-v1 vectors (brute-force cosine
 * similarity; FAISS optional externally).
 */
public final class SemanticEmbeddingIndex {

  private final List<IndexedRow> rows;

  private SemanticEmbeddingIndex(List<IndexedRow> rows) {
    this.rows = List.copyOf(rows);
  }

  /**
   * Builds an index from all embeddings in a training dataset.
   *
   * @param dataset multi-project training dataset
   * @return embedding index for nearest-neighbor search
   */
  public static SemanticEmbeddingIndex fromDataset(TrainingDataset dataset) {
    List<IndexedRow> rows = new ArrayList<>();
    for (TrainingProjectResult project : dataset.projects()) {
      for (SemanticEmbedding embedding : project.embeddings()) {
        rows.add(
            new IndexedRow(
                project.projectPath(),
                embedding.nodeId(),
                embedding.vocabularyPath(),
                embedding.embeddingHex(),
                toUnitVector(embedding.embeddingHex())));
      }
    }
    rows.sort(
        Comparator.comparing(IndexedRow::projectPath)
            .thenComparingLong(IndexedRow::nodeId));
    return new SemanticEmbeddingIndex(rows);
  }

  /**
   * Returns the number of indexed embedding rows.
   *
   * @return number of indexed embedding rows
   */
  public int size() {
    return rows.size();
  }

  /**
   * Top-k neighbors by cosine similarity (excludes exact same project+node when query matches).
   *
   * @param projectPath query project path (excluded from self-match)
   * @param nodeId query node id (excluded from self-match)
   * @param embeddingHex query embedding hex
   * @param k maximum neighbors to return
   * @return ranked neighbors
   */
  public List<EmbeddingNeighbor> nearestNeighbors(
      String projectPath, long nodeId, String embeddingHex, int k) {
    if (k <= 0 || rows.isEmpty()) {
      return List.of();
    }
    double[] query = toUnitVector(embeddingHex);
    List<ScoredRow> scored = new ArrayList<>();
    for (IndexedRow row : rows) {
      if (row.projectPath().equals(projectPath) && row.nodeId() == nodeId) {
        continue;
      }
      scored.add(new ScoredRow(row, cosine(query, row.vector())));
    }
    scored.sort(
        Comparator.comparingDouble(ScoredRow::similarity)
            .reversed()
            .thenComparing(scoredRow -> scoredRow.row().projectPath())
            .thenComparingLong(scoredRow -> scoredRow.row().nodeId()));
    int limit = Math.min(k, scored.size());
    List<EmbeddingNeighbor> neighbors = new ArrayList<>(limit);
    for (int i = 0; i < limit; i++) {
      IndexedRow row = scored.get(i).row();
      neighbors.add(
          new EmbeddingNeighbor(
              row.projectPath(), row.nodeId(), row.vocabularyPath(), scored.get(i).similarity()));
    }
    return List.copyOf(neighbors);
  }

  /**
   * Returns a SHA-256 fingerprint of all indexed rows for reproducibility gates.
   *
   * @return SHA-256 fingerprint of all indexed rows (reproducibility gate)
   */
  public String fingerprint() {
    StringBuilder builder = new StringBuilder();
    for (IndexedRow row : rows) {
      builder.append(row.projectPath()).append('|').append(row.nodeId()).append('|');
      builder.append(row.embeddingHex()).append('\n');
    }
    return TrainingManifestHasher.sha256(builder.toString());
  }

  private static double cosine(double[] left, double[] right) {
    double sum = 0.0;
    for (int i = 0; i < left.length; i++) {
      sum += left[i] * right[i];
    }
    return sum;
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

  private record IndexedRow(
      String projectPath, long nodeId, String vocabularyPath, String embeddingHex, double[] vector) {}

  private record ScoredRow(IndexedRow row, double similarity) {}
}
