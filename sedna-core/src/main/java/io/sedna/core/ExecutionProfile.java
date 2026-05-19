package io.sedna.core;

/**
 * Runtime execution profile. MVP supports DAG only; others are reserved.
 * Ordinal maps to uint8 in DNA header (DAG = 0).
 */
public enum ExecutionProfile {
    DAG(0),
    STATEFUL(1),
    SUPERVISOR(2);

    private final int code;

    ExecutionProfile(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ExecutionProfile fromCode(int code) {
        for (ExecutionProfile profile : values()) {
            if (profile.code == code) {
                return profile;
            }
        }
        throw new IllegalArgumentException("Unknown ExecutionProfile code: " + code);
    }
}
