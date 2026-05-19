package io.sedna.core;

/**
 * Capability identifier with version constraint.
 *
 * @param name capability name (e.g. {@code USER_SERVICE})
 * @param versionConstraint version or range (e.g. {@code 2.1}, {@code >=1.0})
 */
public record CapabilityRef(String name, String versionConstraint) {
    public CapabilityRef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name required");
        }
        if (versionConstraint == null || versionConstraint.isBlank()) {
            throw new IllegalArgumentException("versionConstraint required");
        }
    }

    public String canonical() {
        return name + "@" + versionConstraint;
    }
}
