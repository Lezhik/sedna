package io.sedna.reverse.model;

import java.util.List;

/** Parsed Java type from source (Step 1 output). */
public record ParsedClass(
    String qualifiedName,
    String packageName,
    String simpleName,
    List<String> annotationSimpleNames,
    List<String> dependencyQualifiedNames,
    List<String> publicMethodSignatures) {
  public ParsedClass {
    annotationSimpleNames = List.copyOf(annotationSimpleNames);
    dependencyQualifiedNames = List.copyOf(dependencyQualifiedNames);
    publicMethodSignatures = List.copyOf(publicMethodSignatures);
  }
}
