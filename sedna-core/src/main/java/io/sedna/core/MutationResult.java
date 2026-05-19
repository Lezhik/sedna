package io.sedna.core;

/**
 * Outcome of a mutation transaction.
 */
public record MutationResult(SemanticGraph graph, boolean rolledBack) {}
