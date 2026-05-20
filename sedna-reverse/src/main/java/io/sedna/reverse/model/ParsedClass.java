package io.sedna.reverse.model;

import java.util.List;

/**
 * Parsed Java type from source (Step 1 output).
 *
 * @param qualifiedName fully qualified class name
 * @param packageName Java package name
 * @param simpleName unqualified class name
 * @param annotationSimpleNames simple names of declared annotations
 * @param dependencyQualifiedNames referenced type names from fields and signatures
 * @param publicMethodSignatures public method signatures in canonical form
 */
public record ParsedClass(
    String qualifiedName,
    String packageName,
    String simpleName,
    List<String> annotationSimpleNames,
    List<String> dependencyQualifiedNames,
    List<String> publicMethodSignatures) {

  /** Defensive copy of list components. */
  public ParsedClass {
    annotationSimpleNames = List.copyOf(annotationSimpleNames);
    dependencyQualifiedNames = List.copyOf(dependencyQualifiedNames);
    publicMethodSignatures = List.copyOf(publicMethodSignatures);
  }
}
