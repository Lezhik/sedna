package io.sedna.core;

/** Canonical error codes for {@link Result} boundaries. */
public enum ErrorCode {
    INVALID_DNA,
    UNKNOWN_VOCAB,
    VALIDATION_FAILED,
    NOT_IMPLEMENTED,
    UNSUPPORTED_PROFILE,
    CONTRACT_UNRESOLVED,
    CONSTRAINT_VIOLATION,
    MUTATION_ROLLED_BACK,
    INTERNAL
}
