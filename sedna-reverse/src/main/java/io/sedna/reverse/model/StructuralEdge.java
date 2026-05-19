package io.sedna.reverse.model;

/** Structural dependency edge between parsed classes. */
public record StructuralEdge(String sourceQualifiedName, String targetQualifiedName) {}
