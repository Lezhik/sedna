package io.sedna.core;

/**
 * Resolved semantic definition for a {@link VocabRef} from the registry.
 *
 * @param ref canonical vocabulary reference
 * @param displayName human-readable label
 * @param description optional description
 */
public record SemanticDefinition(VocabRef ref, String displayName, String description) {}
