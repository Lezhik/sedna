package io.sedna.core;

import java.util.List;

/**
 * Single semantic executable unit in the hypergraph.
 */
public record GenomeNode(
        long nodeId,
        NodeKind kind,
        SemanticCore core,
        List<Contract> contracts,
        List<Constraint> constraints) {
    public GenomeNode {
        contracts = List.copyOf(contracts);
        constraints = List.copyOf(constraints);
    }
}
