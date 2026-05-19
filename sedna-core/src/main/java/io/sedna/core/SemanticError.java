package io.sedna.core;

/**
 * Canonical error at module boundaries. {@code nodeId} is {@code 0} for global errors.
 */
public record SemanticError(ErrorCode code, long nodeId, String message) {
    public SemanticError {
        if (code == null) {
            throw new IllegalArgumentException("code required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message required");
        }
    }

    public static SemanticError global(ErrorCode code, String message) {
        return new SemanticError(code, 0L, message);
    }
}
