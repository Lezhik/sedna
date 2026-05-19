package io.sedna.core;

/** Semantic graph mutation operations. */
public enum MutationType {
    NODE_INSERT,
    NODE_DELETE,
    SUBTREE_REPLACE,
    MOTIF_FOLD,
    MOTIF_EXPAND,
    CONTRACT_UPGRADE,
    CONSTRAINT_INJECTION
}
