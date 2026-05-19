package io.sedna.core;

/**
 * Request to mutate a subtree rooted at {@code targetNodeId}.
 */
public record Mutation(long targetNodeId, MutationType operation) {}
