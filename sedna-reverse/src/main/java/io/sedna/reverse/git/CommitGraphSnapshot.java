package io.sedna.reverse.git;

import io.sedna.core.SemanticGraph;

/** Semantic graph captured at a specific Git commit. */
public record CommitGraphSnapshot(String commitHash, SemanticGraph graph) {}
