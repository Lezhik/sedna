package io.sedna.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic ordering utilities for all graph collections.
 * Tie-breaker: lexicographic {@code nodeId} where applicable.
 */
public final class CanonicalOrdering {

    private static final Comparator<VocabRef> VOCAB_REF =
            Comparator.comparing(VocabRef::vocabularyId)
                    .thenComparing(VocabRef::termPath)
                    .thenComparing(VocabRef::version);

    private static final Comparator<CapabilityRef> CAPABILITY_REF =
            Comparator.comparing(CapabilityRef::name).thenComparing(CapabilityRef::versionConstraint);

    private static final Comparator<Contract> CONTRACT =
            Comparator.comparing(CanonicalOrdering::contractSortKey);

    private static final Comparator<Constraint> CONSTRAINT = Comparator.comparing(Constraint::code);

    private static final Comparator<GenomeNode> GENOME_NODE =
            Comparator.comparingLong(GenomeNode::nodeId);

    private static final Comparator<SemanticLink> SEMANTIC_LINK = Comparator.comparingLong(SemanticLink::sourceNodeId)
            .thenComparingLong(SemanticLink::targetNodeId)
            .thenComparing(link -> link.type().name());

    private CanonicalOrdering() {}

    /** Primary comparator for canonical graph operations. */
    public static Comparator<GenomeNode> genomeNodeComparator() {
        return GENOME_NODE;
    }

    public static Comparator<SemanticLink> semanticLinkComparator() {
        return SEMANTIC_LINK;
    }

    public static Comparator<Contract> contractComparator() {
        return CONTRACT;
    }

    public static Comparator<Constraint> constraintComparator() {
        return CONSTRAINT;
    }

    public static Comparator<VocabRef> vocabRefComparator() {
        return VOCAB_REF;
    }

    public static List<GenomeNode> sortNodes(List<GenomeNode> nodes) {
        return nodes.stream().sorted(GENOME_NODE).toList();
    }

    public static List<SemanticLink> sortLinks(List<SemanticLink> links) {
        return links.stream().sorted(SEMANTIC_LINK).toList();
    }

    public static SemanticGraph canonicalize(SemanticGraph graph) {
        List<GenomeNode> sortedNodes = new ArrayList<>(graph.nodes().size());
        for (GenomeNode node : sortNodes(graph.nodes())) {
            sortedNodes.add(new GenomeNode(
                    node.nodeId(),
                    node.kind(),
                    canonicalizeCore(node.core()),
                    node.contracts().stream().sorted(CONTRACT).toList(),
                    node.constraints().stream().sorted(CONSTRAINT).toList()));
        }
        return new SemanticGraph(sortedNodes, sortLinks(graph.links()), graph.vocabularyVersion());
    }

    private static SemanticCore canonicalizeCore(SemanticCore core) {
        return new SemanticCore(
                core.classRef(),
                core.targetRef(),
                core.operationRef(),
                core.modifiers().stream().sorted(VOCAB_REF).toList());
    }

    private static String contractSortKey(Contract contract) {
        String provides =
                contract.provides().stream().map(CapabilityRef::canonical).sorted().reduce((a, b) -> a + "|" + b).orElse("");
        String requires =
                contract.requires().stream().map(CapabilityRef::canonical).sorted().reduce((a, b) -> a + "|" + b).orElse("");
        return provides
                + ">"
                + requires
                + ">"
                + contract.protocol().name()
                + ">"
                + contract.ioSchema().format()
                + ">"
                + contract.ioSchema().payload();
    }
}
