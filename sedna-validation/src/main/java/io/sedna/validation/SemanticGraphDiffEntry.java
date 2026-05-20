package io.sedna.validation;

/** Single semantic graph difference (deterministic ordering). */
public record SemanticGraphDiffEntry(long nodeId, String kind, String payload) {}
