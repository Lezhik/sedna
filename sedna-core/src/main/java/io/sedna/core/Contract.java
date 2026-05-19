package io.sedna.core;

import java.util.List;

/**
 * Semantic interoperability contract (capability-driven).
 */
public record Contract(
        List<CapabilityRef> provides,
        List<CapabilityRef> requires,
        Protocol protocol,
        SchemaRef ioSchema) {
    public Contract {
        provides = List.copyOf(provides);
        requires = List.copyOf(requires);
    }
}
