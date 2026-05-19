package io.sedna.core;

/**
 * Architectural constraint propagated on nodes (e.g. {@code STATELESS_ONLY}).
 *
 * @param code constraint identifier from vocabulary
 */
public record Constraint(String code) {
    public Constraint {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code required");
        }
    }
}
