package io.sedna.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalOrderingTest {

    @Test
    void sortNodesUsesNodeIdLexicographicOrder() {
        GenomeNode low = node(1L);
        GenomeNode high = node(3L);
        GenomeNode mid = node(2L);
        List<GenomeNode> shuffled = new ArrayList<>(List.of(high, low, mid));

        List<GenomeNode> sorted = CanonicalOrdering.sortNodes(shuffled);

        assertEquals(List.of(1L, 2L, 3L), sorted.stream().map(GenomeNode::nodeId).toList());
    }

    @Test
    void canonicalizeGraphIsStableRegardlessOfInputListOrder() {
        SemanticGraph a = graph(List.of(node(3L), node(1L), node(2L)));
        SemanticGraph b = graph(List.of(node(2L), node(3L), node(1L)));

        SemanticGraph canonicalA = CanonicalOrdering.canonicalize(a);
        SemanticGraph canonicalB = CanonicalOrdering.canonicalize(b);

        assertIterableEquals(
                canonicalA.nodes().stream().map(GenomeNode::nodeId).toList(),
                canonicalB.nodes().stream().map(GenomeNode::nodeId).toList());
    }

    @Test
    void sortLinksUsesSourceTargetTypeOrder() {
        SemanticLink linkB = new SemanticLink(2L, 3L, LinkType.DEPENDENCY);
        SemanticLink linkA = new SemanticLink(1L, 2L, LinkType.DEPENDENCY);
        List<SemanticLink> sorted = CanonicalOrdering.sortLinks(List.of(linkB, linkA));
        assertEquals(1L, sorted.getFirst().sourceNodeId());
    }

    private static GenomeNode node(long id) {
        VocabRef ref = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
        SemanticCore core = new SemanticCore(ref, ref, ref, List.of());
        return new GenomeNode(id, NodeKind.ENTITY, core, List.of(), List.of());
    }

    private static SemanticGraph graph(List<GenomeNode> nodes) {
        return new SemanticGraph(nodes, List.of(), new RegistryVersion("core", 1, 0));
    }
}
