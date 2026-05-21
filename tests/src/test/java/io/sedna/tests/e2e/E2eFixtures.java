package io.sedna.tests.e2e;

import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.core.NodeKind;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import io.sedna.dna.DnaServices;
import io.sedna.dna.NodeIdHasher;
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

  public static Path invalidOrphanLinkPath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/invalid-graph.sdna");
  }

  public static Path invalidDuplicateNodePath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/duplicate-node.sdna");
  }

  public static Path invalidUnknownVocabPath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/unknown-vocab.sdna");
  }

  public static Path invalidDnaMagicPath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/invalid-dna-magic.sdna");
  }

  public static Path invalidCyclicDependencyPath() {
    return E2eTestSupport.fixturesRoot().resolve("invalid/cyclic-dependency.sdna");
  }

  public static Path registryCollisionPath() {
    return E2eTestSupport.fixturesRoot().resolve("registry/conflicts/collision.tlv");
  }

  public static Path registryCorruptedPath() {
    return E2eTestSupport.fixturesRoot().resolve("registry/corrupted/corrupt.tlv");
  }

  @Deprecated
  /** @deprecated use {@link #invalidOrphanLinkPath()} */
  public static Path invalidGraphFixturePath() {
    return invalidOrphanLinkPath();
  }

  public static byte[] invalidDuplicateNodeDna() {
    SemanticGraph base = CmsReferenceFixtureGraph.create();
    GenomeNode duplicate =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(
                base.nodes().getFirst().nodeId(),
                NodeKind.ENTITY,
                base.nodes().getFirst().core(),
                List.of(),
                List.of()));
    List<GenomeNode> nodes = new java.util.ArrayList<>(base.nodes());
    nodes.add(duplicate);
    return DnaServices.encoder()
        .encode(new SemanticGraph(nodes, base.links(), base.vocabularyVersion()))
        .value();
  }

  public static byte[] invalidUnknownVocabDna() {
    SemanticGraph base = CmsReferenceFixtureGraph.create();
    GenomeNode entity =
        base.nodes().stream().filter(n -> n.kind() == NodeKind.ENTITY).findFirst().orElseThrow();
    VocabRef unknown = new VocabRef("nope", "DOMAIN.ENTITY.AGGREGATE", "v1");
    SemanticCore badCore =
        new SemanticCore(unknown, entity.core().targetRef(), entity.core().operationRef(), List.of());
    GenomeNode badEntity =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(
                0L, entity.kind(), badCore, entity.contracts(), entity.constraints()));
    long oldEntityId = entity.nodeId();
    long newEntityId = badEntity.nodeId();
    List<GenomeNode> nodes =
        base.nodes().stream().map(n -> n.kind() == NodeKind.ENTITY ? badEntity : n).toList();
    List<SemanticLink> links =
        base.links().stream()
            .map(
                link ->
                    new SemanticLink(
                        remapNodeId(link.sourceNodeId(), oldEntityId, newEntityId),
                        remapNodeId(link.targetNodeId(), oldEntityId, newEntityId),
                        link.type()))
            .toList();
    return DnaServices.encoder()
        .encode(new SemanticGraph(nodes, links, base.vocabularyVersion()))
        .value();
  }

  private static long remapNodeId(long nodeId, long oldId, long newId) {
    return nodeId == oldId ? newId : nodeId;
  }

  public static byte[] invalidDnaMagicBytes() {
    return new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
  }

  public static byte[] invalidCyclicDependencyDna() {
    SemanticGraph base = CmsReferenceFixtureGraph.create();
    long entityId =
        base.nodes().stream().filter(n -> n.kind() == NodeKind.ENTITY).findFirst().orElseThrow().nodeId();
    long controllerId =
        base.nodes().stream()
            .filter(n -> n.kind() == NodeKind.CONTROLLER)
            .findFirst()
            .orElseThrow()
            .nodeId();
    List<SemanticLink> links = new java.util.ArrayList<>(base.links());
    links.add(new SemanticLink(entityId, controllerId, LinkType.DEPENDENCY));
    return DnaServices.encoder()
        .encode(new SemanticGraph(base.nodes(), List.copyOf(links), base.vocabularyVersion()))
        .value();
  }

  public static void materializeAllFixtures() throws IOException {
    writeIfChanged(invalidOrphanLinkPath(), invalidGraphDna());
    writeIfChanged(invalidDuplicateNodePath(), invalidDuplicateNodeDna());
    writeIfChanged(invalidUnknownVocabPath(), invalidUnknownVocabDna());
    writeIfChanged(invalidDnaMagicPath(), invalidDnaMagicBytes());
    writeIfChanged(invalidCyclicDependencyPath(), invalidCyclicDependencyDna());
    writeIfChanged(registryCollisionPath(), registryCollisionExtensionPayload());
    writeIfChanged(registryCorruptedPath(), corruptedRegistryExtensionPayload());
    Path mutationsDir = E2eTestSupport.fixturesRoot().resolve("mutations/sequence-10-valid");
    Files.createDirectories(mutationsDir);
    Files.writeString(
        mutationsDir.resolve("README.md"),
        "Ten-step constraint-injection sequence (STATELESS_ONLY / READ_ONLY alternating). "
            + "Applied programmatically in DeepMutationDriftE2eTest.\n");
  }

  private static void writeIfChanged(Path path, byte[] bytes) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, bytes);
  }

  public static void ensureInvalidGraphFixtureOnDisk() throws IOException {
    materializeAllFixtures();
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
