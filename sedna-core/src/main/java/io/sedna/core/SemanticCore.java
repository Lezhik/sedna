package io.sedna.core;

import java.util.List;

/**
 * Compact semantic intent references — never stores implementation source.
 *
 * @param classRef primary class/role reference
 * @param targetRef target reference
 * @param operationRef operation reference
 * @param modifiers ordered modifier references (canonical order)
 */
public record SemanticCore(
        VocabRef classRef, VocabRef targetRef, VocabRef operationRef, List<VocabRef> modifiers) {
    public SemanticCore {
        modifiers = List.copyOf(modifiers);
    }
}
