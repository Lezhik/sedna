package io.sedna.reverse.spring;

import io.sedna.core.CapabilityRef;
import io.sedna.core.Constraint;
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
import java.util.Locale;
import java.util.Optional;

/** Deterministic semantic mapping for Spring Boot REST monoliths (general profile). */
public final class SpringBootSemanticRules {

  public static final String SOURCE_PACKAGE_CONSTRAINT = "SOURCE_PACKAGE";

  private static final VocabRef ENTITY = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
  private static final VocabRef SERVICE = new VocabRef("core", "DOMAIN.SERVICE.APPLICATION", "v1");
  private static final VocabRef CONTROLLER = new VocabRef("core", "DOMAIN.API.CONTROLLER", "v1");

  private SpringBootSemanticRules() {}

  public static boolean isSpringBootMonolith(StructuralGraph structural) {
    return structural.project().classes().stream()
        .anyMatch(cls -> cls.annotationSimpleNames().contains("SpringBootApplication"));
  }

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

  public static SemanticGraph toSemanticGraph(StructuralGraph structural) {
    String basePackage = detectBasePackage(structural);
    List<GenomeNode> nodes = new ArrayList<>();
    List<SemanticLink> links = new ArrayList<>();

    ParsedClass entityClass = findByKind(structural, NodeKind.ENTITY).orElseThrow();
    ParsedClass serviceClass = findByKind(structural, NodeKind.SERVICE).orElseThrow();
    ParsedClass controllerClass = findByKind(structural, NodeKind.CONTROLLER).orElseThrow();

    Constraint packageConstraint = new Constraint(SOURCE_PACKAGE_CONSTRAINT + ":" + basePackage);

    GenomeNode entityNode = toEntityNode(entityClass, packageConstraint);
    GenomeNode serviceNode = toServiceNode(serviceClass, entityClass, packageConstraint);
    GenomeNode controllerNode = toControllerNode(controllerClass, packageConstraint);

    nodes.add(entityNode);
    nodes.add(serviceNode);
    nodes.add(controllerNode);

    links.addAll(
        inferLinks(structural, controllerClass, serviceClass, controllerNode, serviceNode, entityNode));

    links.sort(
        Comparator.comparingLong(SemanticLink::sourceNodeId)
            .thenComparingLong(SemanticLink::targetNodeId)
            .thenComparing(link -> link.type().name()));

    return new SemanticGraph(nodes, links, new RegistryVersion("core", 1, 0));
  }

  private static String detectBasePackage(StructuralGraph structural) {
    return structural.project().classes().stream()
        .filter(cls -> cls.annotationSimpleNames().contains("SpringBootApplication"))
        .map(ParsedClass::packageName)
        .sorted()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing @SpringBootApplication class"));
  }

  private static Optional<ParsedClass> findByKind(StructuralGraph structural, NodeKind kind) {
    return structural.project().classes().stream()
        .filter(cls -> classify(cls).orElse(null) == kind)
        .sorted(Comparator.comparing(ParsedClass::qualifiedName))
        .findFirst();
  }

  private static GenomeNode toEntityNode(ParsedClass parsed, Constraint packageConstraint) {
    SemanticCore core = new SemanticCore(ENTITY, ENTITY, ENTITY, List.of());
    String signature = "class:" + parsed.qualifiedName();
    Contract contract =
        new Contract(
            List.of(capabilityFor(parsed.simpleName(), "REPOSITORY")),
            List.of(),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, signature));
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(
            0L, NodeKind.ENTITY, core, List.of(contract), List.of(packageConstraint)));
  }

  private static GenomeNode toServiceNode(
      ParsedClass parsed, ParsedClass entityClass, Constraint packageConstraint) {
    SemanticCore core = new SemanticCore(SERVICE, ENTITY, ENTITY, List.of());
    String signature = primaryMethodSignature(parsed);
    Contract contract =
        new Contract(
            List.of(capabilityFor(parsed.simpleName(), "SERVICE")),
            List.of(capabilityFor(entityClass.simpleName(), "REPOSITORY", ">=1.0")),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, signature));
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(
            0L, NodeKind.SERVICE, core, List.of(contract), List.of(packageConstraint)));
  }

  private static GenomeNode toControllerNode(ParsedClass parsed, Constraint packageConstraint) {
    SemanticCore core = new SemanticCore(CONTROLLER, SERVICE, ENTITY, List.of());
    Contract schemaHolder =
        new Contract(
            List.of(),
            List.of(),
            Protocol.SYNC,
            new SchemaRef(SchemaRef.JAVA_SIGNATURE, "class:" + parsed.qualifiedName()));
    return NodeIdHasher.withCanonicalNodeId(
        new GenomeNode(
            0L, NodeKind.CONTROLLER, core, List.of(schemaHolder), List.of(packageConstraint)));
  }

  private static CapabilityRef capabilityFor(String simpleName, String suffix) {
    return new CapabilityRef(simpleName.toUpperCase(Locale.ROOT) + "_" + suffix, "1.0");
  }

  private static CapabilityRef capabilityFor(String simpleName, String suffix, String version) {
    return new CapabilityRef(simpleName.toUpperCase(Locale.ROOT) + "_" + suffix, version);
  }

  private static String primaryMethodSignature(ParsedClass parsed) {
    return parsed.publicMethodSignatures().stream()
        .sorted()
        .findFirst()
        .orElse("void handle()");
  }

  private static List<SemanticLink> inferLinks(
      StructuralGraph structural,
      ParsedClass controller,
      ParsedClass service,
      GenomeNode controllerNode,
      GenomeNode serviceNode,
      GenomeNode entityNode) {
    boolean controllerToService =
        structural.edges().stream()
            .anyMatch(
                edge ->
                    edge.sourceQualifiedName().equals(controller.qualifiedName())
                        && edge.targetQualifiedName().equals(service.qualifiedName()));
    if (!controllerToService) {
      throw new IllegalStateException(
          "Missing controller -> service structural edge between "
              + controller.qualifiedName()
              + " and "
              + service.qualifiedName());
    }
    List<SemanticLink> links = new ArrayList<>();
    links.add(new SemanticLink(controllerNode.nodeId(), serviceNode.nodeId(), LinkType.DEPENDENCY));
    links.add(new SemanticLink(serviceNode.nodeId(), entityNode.nodeId(), LinkType.DEPENDENCY));
    return links;
  }
}
