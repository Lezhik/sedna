package io.sedna.forward.model;

import io.sedna.core.SemanticGraph;

/** Resolved semantic hypergraph; LINKS remain semantic references until contract resolution. */
public record SemanticHypergraph(SemanticGraph graph) {}
