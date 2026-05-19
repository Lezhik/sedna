package io.sedna.training.model;

/** Deterministic semantic embedding (SEDNA-EMBED-v1: vocabulary path + contract signature). */
public record SemanticEmbedding(long nodeId, String vocabularyPath, String embeddingHex) {}
