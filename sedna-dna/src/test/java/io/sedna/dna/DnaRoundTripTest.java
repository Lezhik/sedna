package io.sedna.dna;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.CapabilityRef;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.NodeKind;
import io.sedna.core.Protocol;
import io.sedna.core.RegistryVersion;
import io.sedna.core.Result;
import io.sedna.core.SchemaRef;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** FR-dna.02 / FR-dna.03 round-trip and byte identity tests. */
class DnaRoundTripTest {

  private final DnaEncoder encoder = DnaServices.encoder();
  private final DnaDecoder decoder = DnaServices.decoder();

  @Test
  void encodeDecodeRoundTrip() {
    SemanticGraph original = sampleGraph();
    byte[] dna = encoder.encode(original).value();
    SemanticGraph decoded = decoder.decode(dna).value();
    assertEquals(original.nodes().size(), decoded.nodes().size());
    assertEquals(original.links().size(), decoded.links().size());
    assertEquals(
        original.nodes().stream().map(GenomeNode::nodeId).toList(),
        decoded.nodes().stream().map(GenomeNode::nodeId).toList());
  }

  @Test
  void canonicallyEqualGraphsProduceIdenticalBytes() {
    SemanticGraph graphA = sampleGraph();
    List<GenomeNode> shuffled = new ArrayList<>(graphA.nodes());
    java.util.Collections.reverse(shuffled);
    SemanticGraph graphB = new SemanticGraph(shuffled, graphA.links(), graphA.vocabularyVersion());

    byte[] dnaA = encoder.encode(graphA).value();
    byte[] dnaB = encoder.encode(graphB).value();
    assertArrayEquals(dnaA, dnaB);
  }

  @Test
  void reencodeIsByteIdentical() {
    SemanticGraph graph = sampleGraph();
    byte[] first = encoder.encode(graph).value();
    SemanticGraph decoded = decoder.decode(first).value();
    byte[] second = encoder.encode(decoded).value();
    assertArrayEquals(first, second);
  }

  @Test
  void invalidMagicFails() {
    Result<SemanticGraph, SemanticError> result = decoder.decode(new byte[] {0, 1, 2, 3, 4, 5});
    assertTrue(!result.isOk());
    assertTrue(result.error().message().toLowerCase(Locale.ROOT).contains("magic"));
  }

  private static SemanticGraph sampleGraph() {
    VocabRef entity = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
    VocabRef service = new VocabRef("core", "DOMAIN.SERVICE.APPLICATION", "v1");
    VocabRef controller = new VocabRef("core", "DOMAIN.API.CONTROLLER", "v1");

    SemanticCore entityCore = new SemanticCore(entity, entity, entity, List.of());
    SemanticCore serviceCore = new SemanticCore(service, entity, entity, List.of());
    SemanticCore controllerCore = new SemanticCore(controller, service, entity, List.of());

    Contract serviceContract =
        new Contract(
            List.of(new CapabilityRef("USER_SERVICE", "1.0")),
            List.of(new CapabilityRef("USER_REPOSITORY", ">=1.0")),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, "void handle()"));

    GenomeNode entityNode = new GenomeNode(1L, NodeKind.ENTITY, entityCore, List.of(), List.of());
    GenomeNode serviceNode =
        new GenomeNode(2L, NodeKind.SERVICE, serviceCore, List.of(serviceContract), List.of());
    GenomeNode controllerNode =
        new GenomeNode(3L, NodeKind.CONTROLLER, controllerCore, List.of(), List.of());

    SemanticLink link = new SemanticLink(3L, 2L, LinkType.DEPENDENCY);

    return new SemanticGraph(
        List.of(entityNode, serviceNode, controllerNode),
        List.of(link),
        new RegistryVersion("core", 1, 0));
  }
}
