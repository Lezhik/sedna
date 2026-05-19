package io.sedna.forward.model;

import io.sedna.core.CapabilityRef;

/** Physical capability binding edge materialized during contract resolution. */
public record MaterializedEdge(long consumerNodeId, long providerNodeId, CapabilityRef capability) {}
