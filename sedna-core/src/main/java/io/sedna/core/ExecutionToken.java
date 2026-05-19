package io.sedna.core;

import java.util.Arrays;

/**
 * Deterministic execution token hash (32 bytes SHA-256).
 */
public record ExecutionToken(byte[] tokenHash) {
    private static final int HASH_LENGTH = 32;

    public ExecutionToken {
        if (tokenHash == null || tokenHash.length != HASH_LENGTH) {
            throw new IllegalArgumentException("tokenHash must be 32 bytes");
        }
        tokenHash = Arrays.copyOf(tokenHash, HASH_LENGTH);
    }

    @Override
    public byte[] tokenHash() {
        return Arrays.copyOf(tokenHash, tokenHash.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExecutionToken that)) {
            return false;
        }
        return Arrays.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tokenHash);
    }
}
