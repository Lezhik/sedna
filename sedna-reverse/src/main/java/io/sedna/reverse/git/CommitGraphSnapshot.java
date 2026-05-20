package io.sedna.reverse.git;

import io.sedna.core.SemanticGraph;

/**
 * Semantic graph captured at a specific Git commit.
 *
 * @param commitHash full Git commit hash
 * @param graph semantic graph at that commit
 */
public record CommitGraphSnapshot(String commitHash, SemanticGraph graph) {}
