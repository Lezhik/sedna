package io.sedna.core;

/**
 * Directed semantic link between two nodes.
 */
public record SemanticLink(long sourceNodeId, long targetNodeId, LinkType type) {}
