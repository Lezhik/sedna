package io.sedna.training.model;

import io.sedna.core.MutationType;

/**
 * Labeled mutation candidate for training (not applied).
 *
 * @param targetNodeId genome node targeted by the mutation
 * @param operation mutation operation type
 * @param label deterministic label (may include commit hash prefix)
 */
public record MutationDatasetEntry(long targetNodeId, MutationType operation, String label) {}
