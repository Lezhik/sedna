package io.sedna.core;

/**
 * Semantic node kinds. Ordinal order is stable for binary serialization (uint16).
 */
public enum NodeKind {
    ENTITY(0),
    SERVICE(1),
    WORKFLOW(2),
    POLICY(3),
    CONTROLLER(4),
    INTEGRATION(5),
    MOTIF(6);

    private final int code;

    NodeKind(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static NodeKind fromCode(int code) {
        for (NodeKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown NodeKind code: " + code);
    }
}
