package io.sedna.core;

/**
 * IO schema reference for a contract.
 *
 * @param format {@code JSON_SCHEMA} or {@code JAVA_SIGNATURE}
 * @param payload inline schema or type signature, or registry key
 */
public record SchemaRef(String format, String payload) {
    public static final String JSON_SCHEMA = "JSON_SCHEMA";
    public static final String JAVA_SIGNATURE = "JAVA_SIGNATURE";

    public SchemaRef {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("format required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload required");
        }
    }
}
