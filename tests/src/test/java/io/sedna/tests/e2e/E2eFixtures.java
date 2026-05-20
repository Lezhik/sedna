package io.sedna.tests.e2e;

import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.core.NodeKind;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.registry.EmbeddedCoreVocabulary;
import io.sedna.registry.TlvRegistryExtensionDecoder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds binary/text fixtures for E2E tests. */
public final class E2eFixtures {

  private E2eFixtures() {}

  public static byte[] invalidGraphDna() {
    SemanticGraph base = CmsReferenceFixtureGraph.create();
    long missingTarget = 0xDEADBEEFL;
    List<SemanticLink> links =
        List.of(
            new SemanticLink(
                base.nodes().stream()
                    .filter(n -> n.kind() == NodeKind.CONTROLLER)
                    .findFirst()
                    .orElseThrow()
                    .nodeId(),
                missingTarget,
                LinkType.DEPENDENCY));
    SemanticGraph invalid = new SemanticGraph(base.nodes(), links, base.vocabularyVersion());
    return DnaServices.encoder().encode(invalid).value();
  }

  public static Path invalidGraphFixturePath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/invalid-graph.sdna");
  }

  public static void ensureInvalidGraphFixtureOnDisk() throws IOException {
    Path path = invalidGraphFixturePath();
    Files.createDirectories(path.getParent());
    if (!Files.isRegularFile(path)) {
      Files.write(path, invalidGraphDna());
    }
  }

  public static byte[] registryCollisionExtensionPayload() {
    Map.Entry<String, SemanticDefinition> coreEntry =
        EmbeddedCoreVocabulary.load().entrySet().stream()
            .min(Map.Entry.comparingByKey())
            .orElseThrow();
    SemanticDefinition collision =
        new SemanticDefinition(coreEntry.getValue().ref(), "Collision", "E2E collision entry");
    return TlvRegistryExtensionDecoder.encode(Map.of(coreEntry.getKey(), collision));
  }

  public static byte[] corruptedRegistryExtensionPayload() {
    return new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
  }

  public static Mutation addPaymentModuleMutation(SemanticGraph graph) {
    long serviceId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.SERVICE)
            .findFirst()
            .orElseThrow()
            .nodeId();
    return new Mutation(
        serviceId,
        MutationType.CONSTRAINT_INJECTION,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(new Constraint("STATELESS_ONLY")));
  }

  public static Mutation invalidCrossDomainMutation(SemanticGraph graph) {
    long controllerId =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();
    GenomeNode entity =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.ENTITY)
            .findFirst()
            .orElseThrow();
    GenomeNode tamperedEntity =
        new GenomeNode(
            entity.nodeId(),
            entity.kind(),
            entity.core(),
            entity.contracts(),
            List.of(new Constraint("E2E_CROSS_DOMAIN_TAMPER")));
    GenomeNode controller =
        graph.nodes().stream()
            .filter(node -> node.nodeId() == controllerId)
            .findFirst()
            .orElseThrow();
    SemanticGraph replacement =
        new SemanticGraph(
            List.of(controller, tamperedEntity),
            graph.links().stream()
                .filter(
                    link ->
                        link.sourceNodeId() == controllerId
                            || link.targetNodeId() == controllerId
                            || link.sourceNodeId() == entity.nodeId()
                            || link.targetNodeId() == entity.nodeId())
                .toList(),
            graph.vocabularyVersion());
    return new Mutation(
        controllerId,
        MutationType.SUBTREE_REPLACE,
        Optional.empty(),
        Optional.of(replacement),
        Optional.empty(),
        Optional.empty());
  }
}
