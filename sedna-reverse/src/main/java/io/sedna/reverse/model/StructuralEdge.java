package io.sedna.reverse.model;

/**
 * Structural dependency edge between parsed classes.
 *
 * @param sourceQualifiedName dependent class qualified name
 * @param targetQualifiedName referenced class qualified name
 */
public record StructuralEdge(String sourceQualifiedName, String targetQualifiedName) {}
