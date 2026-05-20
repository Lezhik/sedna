package io.sedna.reverse.structural;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.ParsedProject;
import io.sedna.reverse.model.StructuralEdge;
import io.sedna.reverse.model.StructuralGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Step 2 — build structural dependency graph and detect cycles. */
public final class StructuralGraphStep {

  /** Creates a structural graph builder. */
  public StructuralGraphStep() {}

  /**
   * Builds a directed class dependency graph and rejects cyclic components.
   *
   * @param project parsed project model
   * @return structural graph or validation error when a cycle is detected
   */
  public Result<StructuralGraph, SemanticError> build(ParsedProject project) {
    List<StructuralEdge> edges = new ArrayList<>();
    Map<String, List<String>> adjacency = new TreeMap<>();

    for (ParsedClass parsed : project.classes()) {
      adjacency.putIfAbsent(parsed.qualifiedName(), new ArrayList<>());
      for (String dependency : parsed.dependencyQualifiedNames()) {
        ParsedClass target = project.classesByName().get(dependency);
        if (target == null) {
          continue;
        }
        edges.add(new StructuralEdge(parsed.qualifiedName(), target.qualifiedName()));
        adjacency.get(parsed.qualifiedName()).add(target.qualifiedName());
      }
    }

    edges.sort(
        Comparator.comparing(StructuralEdge::sourceQualifiedName)
            .thenComparing(StructuralEdge::targetQualifiedName));

    List<String> nodes = project.classes().stream().map(ParsedClass::qualifiedName).sorted().toList();
    List<List<String>> components = TarjanScc.stronglyConnectedComponents(nodes, adjacency);
    for (List<String> component : components) {
      if (component.size() > 1) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "Structural cycle detected: " + String.join(", ", component)));
      }
    }

    return Result.ok(new StructuralGraph(project, edges));
  }
}
