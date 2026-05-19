package io.sedna.forward.util;

import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticGraph;
import java.util.Optional;

/** Resolves Java names from Spring Boot reverse profile constraints and schemas. */
public final class SpringBootNaming {

  public static final String SOURCE_PACKAGE_PREFIX = "SOURCE_PACKAGE:";
  public static final String SOURCE_CLASS_PREFIX = "SOURCE_CLASS:";

  private SpringBootNaming() {}

  public static boolean hasSourcePackage(SemanticGraph graph) {
    return resolveBasePackage(graph).isPresent();
  }

  public static Optional<String> resolveBasePackage(SemanticGraph graph) {
    return graph.nodes().stream()
        .flatMap(node -> node.constraints().stream())
        .map(Constraint::code)
        .filter(code -> code.startsWith(SOURCE_PACKAGE_PREFIX))
        .map(code -> code.substring(SOURCE_PACKAGE_PREFIX.length()))
        .sorted()
        .findFirst();
  }

  public static String qualifiedClassName(GenomeNode node, String basePackage) {
    return node.contracts().stream()
        .map(contract -> contract.ioSchema())
        .filter(schema -> schema.payload().startsWith("class:"))
        .map(schema -> schema.payload().substring("class:".length()))
        .sorted()
        .findFirst()
        .orElseGet(
            () -> resolveSourceClass(node).orElseGet(() -> defaultQualifiedName(node, basePackage)));
  }

  public static Optional<String> resolveSourceClass(GenomeNode node) {
    return node.constraints().stream()
        .map(Constraint::code)
        .filter(code -> code.startsWith(SOURCE_CLASS_PREFIX))
        .map(code -> code.substring(SOURCE_CLASS_PREFIX.length()))
        .sorted()
        .findFirst();
  }

  private static String defaultQualifiedName(GenomeNode node, String basePackage) {
    String simple =
        switch (node.kind()) {
          case ENTITY -> "Entity";
          case SERVICE -> "ApplicationService";
          case CONTROLLER -> "ApiController";
          default -> node.kind().name() + node.nodeId();
        };
    return basePackage + "." + subPackage(node) + "." + simple;
  }

  public static String subPackage(GenomeNode node) {
    return switch (node.kind()) {
      case ENTITY -> "domain";
      case SERVICE -> "service";
      case CONTROLLER -> "web";
      default -> "generated";
    };
  }

  public static String simpleClassName(GenomeNode node) {
    String qualified = qualifiedClassNameFromSchema(node).orElse("");
    if (!qualified.isBlank()) {
      int idx = qualified.lastIndexOf('.');
      return idx >= 0 ? qualified.substring(idx + 1) : qualified;
    }
    return "Generated" + node.kind().name();
  }

  private static Optional<String> qualifiedClassNameFromSchema(GenomeNode node) {
    return node.contracts().stream()
        .map(contract -> contract.ioSchema())
        .filter(schema -> schema.payload().startsWith("class:"))
        .map(schema -> schema.payload().substring("class:".length()))
        .findFirst();
  }
}
