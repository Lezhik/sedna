package io.sedna.training.model;

/**
 * Ranked embedding retrieval hit (deterministic tie-breakers).
 *
 * @param projectPath absolute project path of the neighbor
 * @param nodeId canonical genome node id
 * @param vocabularyPath SEDNA vocabulary path used for embedding
 * @param similarity cosine similarity score in {@code [0, 1]}
 */
public record EmbeddingNeighbor(
    String projectPath, long nodeId, String vocabularyPath, double similarity) {}
