package io.sedna.core;

/**
 * Version stamp for vocabulary / registry compatibility on a graph.
 *
 * @param vocabularyId primary vocabulary namespace
 * @param major major version
 * @param minor minor version
 */
public record RegistryVersion(String vocabularyId, int major, int minor) {
    public RegistryVersion {
        if (vocabularyId == null || vocabularyId.isBlank()) {
            throw new IllegalArgumentException("vocabularyId required");
        }
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("version components must be non-negative");
        }
    }

    public String canonical() {
        return vocabularyId + ":" + major + "." + minor;
    }
}
