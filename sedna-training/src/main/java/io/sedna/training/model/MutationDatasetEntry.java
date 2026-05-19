package io.sedna.training.model;

import io.sedna.core.MutationType;

/** Labeled mutation candidate for training (not applied). */
public record MutationDatasetEntry(long targetNodeId, MutationType operation, String label) {}
