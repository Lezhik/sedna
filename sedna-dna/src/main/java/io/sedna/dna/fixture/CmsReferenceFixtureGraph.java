package io.sedna.dna.fixture;

import io.sedna.core.CapabilityRef;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.NodeKind;
import io.sedna.core.Protocol;
import io.sedna.core.RegistryVersion;
import io.sedna.core.SchemaRef;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import io.sedna.dna.NodeIdHasher;
import java.util.List;

/**
 * Minimum CMS reference fixture per TODO spec: 1 ENTITY + 1 SERVICE + 1 CONTROLLER, one DEPENDENCY
 * link, one CONTRACT. NodeIDs derived from {@link NodeIdHasher}.
 */
public final class CmsReferenceFixtureGraph {

  private CmsReferenceFixtureGraph() {}

  public static SemanticGraph create() {
    VocabRef entity = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
    VocabRef service = new VocabRef("core", "DOMAIN.SERVICE.APPLICATION", "v1");
    VocabRef controller = new VocabRef("core", "DOMAIN.API.CONTROLLER", "v1");

    SemanticCore entityCore = new SemanticCore(entity, entity, entity, List.of());
    SemanticCore serviceCore = new SemanticCore(service, entity, entity, List.of());
    SemanticCore controllerCore = new SemanticCore(controller, service, entity, List.of());

    Contract entityContract =
        new Contract(
            List.of(new CapabilityRef("USER_REPOSITORY", "1.0")),
            List.of(),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, "interface UserRepository"));

    Contract serviceContract =
        new Contract(
            List.of(new CapabilityRef("USER_SERVICE", "1.0")),
            List.of(new CapabilityRef("USER_REPOSITORY", ">=1.0")),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, "void handle()"));

    GenomeNode entityNode =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(0L, NodeKind.ENTITY, entityCore, List.of(entityContract), List.of()));
    GenomeNode serviceNode =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(
                0L, NodeKind.SERVICE, serviceCore, List.of(serviceContract), List.of()));
    GenomeNode controllerNode =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(0L, NodeKind.CONTROLLER, controllerCore, List.of(), List.of()));

    SemanticLink controllerToService =
        new SemanticLink(controllerNode.nodeId(), serviceNode.nodeId(), LinkType.DEPENDENCY);
    SemanticLink serviceToEntity =
        new SemanticLink(serviceNode.nodeId(), entityNode.nodeId(), LinkType.DEPENDENCY);

    return new SemanticGraph(
        List.of(entityNode, serviceNode, controllerNode),
        List.of(controllerToService, serviceToEntity),
        new RegistryVersion("core", 1, 0));
  }
}
