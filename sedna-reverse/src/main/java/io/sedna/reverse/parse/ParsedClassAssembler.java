package io.sedna.reverse.parse;

import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.ParsedProject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Resolves simple type names to qualified project types (shared by Spoon/JavaParser). */
final class ParsedClassAssembler {

  private ParsedClassAssembler() {}

  static ParsedProject resolveDependencies(ParsedProject project) {
    Map<String, String> simpleToQualified = new TreeMap<>();
    for (ParsedClass parsed : project.classes()) {
      simpleToQualified.put(parsed.simpleName(), parsed.qualifiedName());
    }

    Map<String, ParsedClass> resolved = new TreeMap<>();
    for (ParsedClass parsed : project.classes()) {
      List<String> dependencies = new ArrayList<>();
      for (String dependency : parsed.dependencyQualifiedNames()) {
        String resolvedName = resolveDependencyName(dependency, simpleToQualified, project.classesByName());
        if (project.classesByName().containsKey(resolvedName)) {
          dependencies.add(resolvedName);
        }
      }
      dependencies = dependencies.stream().distinct().sorted().toList();
      resolved.put(
          parsed.qualifiedName(),
          new ParsedClass(
              parsed.qualifiedName(),
              parsed.packageName(),
              parsed.simpleName(),
              parsed.annotationSimpleNames(),
              dependencies,
              parsed.publicMethodSignatures()));
    }
    return new ParsedProject(project.projectRoot(), resolved);
  }

  private static String resolveDependencyName(
      String dependency, Map<String, String> simpleToQualified, Map<String, ParsedClass> classesByName) {
    if (classesByName.containsKey(dependency)) {
      return dependency;
    }
    String qualified = simpleToQualified.get(dependency);
    return qualified != null ? qualified : dependency;
  }
}
