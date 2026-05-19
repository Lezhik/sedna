package io.sedna.core;

/**
 * Reference into the semantic registry vocabulary.
 *
 * @param vocabularyId vocabulary namespace (e.g. {@code core})
 * @param termPath dotted term path (e.g. {@code DOMAIN.ENTITY.AGGREGATE})
 * @param version semantic version string (e.g. {@code v1})
 */
public record VocabRef(String vocabularyId, String termPath, String version) {
    public VocabRef {
        if (vocabularyId == null || vocabularyId.isBlank()) {
            throw new IllegalArgumentException("vocabularyId required");
        }
        if (termPath == null || termPath.isBlank()) {
            throw new IllegalArgumentException("termPath required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version required");
        }
    }

    /** Canonical string form: {@code core:DOMAIN.ENTITY.AGGREGATE:v1}. */
    public String canonicalKey() {
        return vocabularyId + ":" + termPath + ":" + version;
    }
}
