package io.sedna.core;

import java.util.List;

/**
 * Canonical semantic hypergraph DTO.
 */
public record SemanticGraph(
        List<GenomeNode> nodes, List<SemanticLink> links, RegistryVersion vocabularyVersion) {
    public SemanticGraph {
        nodes = List.copyOf(nodes);
        links = List.copyOf(links);
    }
}
