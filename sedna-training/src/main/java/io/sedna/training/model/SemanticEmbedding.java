package io.sedna.training.model;

/**
 * Deterministic semantic embedding (SEDNA-EMBED-v1: vocabulary path + contract signature).
 *
 * @param nodeId canonical genome node id
 * @param vocabularyPath SEDNA vocabulary path key
 * @param embeddingHex SHA-256 hex embedding vector
 */
public record SemanticEmbedding(long nodeId, String vocabularyPath, String embeddingHex) {}
