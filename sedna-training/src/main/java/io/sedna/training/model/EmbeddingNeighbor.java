package io.sedna.training.model;

/** Ranked embedding retrieval hit (deterministic tie-breakers). */
public record EmbeddingNeighbor(
    String projectPath, long nodeId, String vocabularyPath, double similarity) {}
