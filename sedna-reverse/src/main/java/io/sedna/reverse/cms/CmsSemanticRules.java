package io.sedna.reverse.cms;

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
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.StructuralGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Deterministic semantic mapping for {@code examples/sedna-cms/cms-reference}. */
public final class CmsSemanticRules {

  private static final VocabRef ENTITY = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
  private static final VocabRef SERVICE = new VocabRef("core", "DOMAIN.SERVICE.APPLICATION", "v1");
  private static final VocabRef CONTROLLER = new VocabRef("core", "DOMAIN.API.CONTROLLER", "v1");

  private CmsSemanticRules() {}

  /**
   * Returns {@code true} when the project matches the CMS reference profile.
   *
   * @param structural class-level dependency graph
   * @return {@code true} for CMS reference projects
   */
  public static boolean isCmsReference(StructuralGraph structural) {
    return structural.project().classes().stream()
        .anyMatch(cls -> cls.qualifiedName().startsWith("io.sedna.cms."));
  }

  /**
   * Maps a parsed class to a semantic node kind, if recognized.
   *
   * @param parsed parsed Java class
   * @return node kind when the class maps to ENTITY, SERVICE, or CONTROLLER
   */
  public static Optional<NodeKind> classify(ParsedClass parsed) {
    if (parsed.annotationSimpleNames().contains("SpringBootApplication")) {
      return Optional.empty();
    }
    if (parsed.annotationSimpleNames().contains("RestController")
        || parsed.annotationSimpleNames().contains("Controller")) {
      return Optional.of(NodeKind.CONTROLLER);
    }
    if (parsed.annotationSimpleNames().contains("Service")) {
      return Optional.of(NodeKind.SERVICE);
    }
    if (parsed.packageName().endsWith(".domain")) {
      return Optional.of(NodeKind.ENTITY);
    }
    return Optional.empty();
  }

  /**
   * Builds the CMS reference semantic graph from a structural graph.
   *
   * @param structural CMS structural graph
   * @return canonical CMS semantic graph
   */
  public static SemanticGraph toSemanticGraph(StructuralGraph structural) {
    List<GenomeNode> nodes = new ArrayList<>();
    List<SemanticLink> links = new ArrayList<>();

    ParsedClass entityClass = findByKind(structural, NodeKind.ENTITY).orElseThrow();
    ParsedClass serviceClass = findByKind(structural, NodeKind.SERVICE).orElseThrow();
    ParsedClass controllerClass = findByKind(structural, NodeKind.CONTROLLER).orElseThrow();

    GenomeNode entityNode = toEntityNode(entityClass);
    GenomeNode serviceNode = toServiceNode(serviceClass);
    GenomeNode controllerNode = toControllerNode(controllerClass);

    nodes.add(entityNode);
    nodes.add(serviceNode);
    nodes.add(controllerNode);

    links.addAll(inferLinks(structural, controllerNode, serviceNode, entityNode));

    links.sort(
        Comparator.comparingLong(SemanticLink::sourceNodeId)
            .thenComparingLong(SemanticLink::targetNodeId)
            .thenComparing(link -> link.type().name()));

    return new SemanticGraph(nodes, links, new RegistryVersion("core", 1, 0));
  }

  private static Optional<ParsedClass> findByKind(StructuralGraph structural, NodeKind kind) {
    return structural.project().classes().stream()
        .filter(cls -> classify(cls).orElse(null) == kind)
        .findFirst();
  }

  private static GenomeNode toEntityNode(ParsedClass parsed) {
    SemanticCore core = new SemanticCore(ENTITY, ENTITY, ENTITY, List.of());
    Contract contract =
        new Contract(
            List.of(new CapabilityRef("USER_REPOSITORY", "1.0")),
            List.of(),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, "interface UserRepository"));
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(0L, NodeKind.ENTITY, core, List.of(contract), List.of()));
  }

  private static GenomeNode toServiceNode(ParsedClass parsed) {
    SemanticCore core = new SemanticCore(SERVICE, ENTITY, ENTITY, List.of());
    String signature = "void handle()";
    Contract contract =
        new Contract(
            List.of(new CapabilityRef("USER_SERVICE", "1.0")),
            List.of(new CapabilityRef("USER_REPOSITORY", ">=1.0")),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, signature));
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(0L, NodeKind.SERVICE, core, List.of(contract), List.of()));
  }

  private static GenomeNode toControllerNode(ParsedClass parsed) {
    SemanticCore core = new SemanticCore(CONTROLLER, SERVICE, ENTITY, List.of());
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(0L, NodeKind.CONTROLLER, core, List.of(), List.of()));
  }

  private static List<SemanticLink> inferLinks(
      StructuralGraph structural,
      GenomeNode controller,
      GenomeNode service,
      GenomeNode entity) {
    List<SemanticLink> links = new ArrayList<>();
    boolean controllerToService =
        structural.edges().stream()
            .anyMatch(
                edge ->
                    edge.sourceQualifiedName().contains("UserController")
                        && edge.targetQualifiedName().contains("UserService"));
    if (!controllerToService) {
      throw new IllegalStateException("Missing UserController -> UserService structural edge");
    }
    links.add(new SemanticLink(controller.nodeId(), service.nodeId(), LinkType.DEPENDENCY));
    links.add(new SemanticLink(service.nodeId(), entity.nodeId(), LinkType.DEPENDENCY));
    return links;
  }
}
