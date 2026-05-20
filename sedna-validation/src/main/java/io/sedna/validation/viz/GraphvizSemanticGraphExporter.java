package io.sedna.validation.viz;

import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.Comparator;

/** Graphviz DOT export for semantic graphs (Phase 14 visualization). */
public final class GraphvizSemanticGraphExporter {

  private GraphvizSemanticGraphExporter() {}

  public static String toDot(SemanticGraph graph, String graphId) {
    String safeId = sanitizeId(graphId);
    StringBuilder builder = new StringBuilder();
    builder.append("digraph ").append(safeId).append(" {\n");
    builder.append("  rankdir=LR;\n");
    builder.append("  node [shape=box, fontname=Helvetica];\n");

    graph.nodes().stream()
        .sorted(Comparator.comparingLong(GenomeNode::nodeId))
        .forEach(
            node ->
                builder
                    .append("  n")
                    .append(node.nodeId())
                    .append(" [label=\"")
                    .append(escape(node.kind().name()))
                    .append("\\n")
                    .append(node.nodeId())
                    .append("\"];\n"));

    graph.links().stream()
        .sorted(
            Comparator.comparingLong(SemanticLink::sourceNodeId)
                .thenComparingLong(SemanticLink::targetNodeId)
                .thenComparing(link -> link.type().name()))
        .forEach(
            link ->
                builder
                    .append("  n")
                    .append(link.sourceNodeId())
                    .append(" -> n")
                    .append(link.targetNodeId())
                    .append(" [label=\"")
                    .append(escape(link.type().name()))
                    .append("\"];\n"));

    builder.append("}\n");
    return builder.toString();
  }

  private static String sanitizeId(String graphId) {
    String sanitized = graphId.replaceAll("[^A-Za-z0-9_]", "_");
    if (sanitized.isBlank()) {
      return "sedna_graph";
    }
    if (Character.isDigit(sanitized.charAt(0))) {
      return "g_" + sanitized;
    }
    return sanitized;
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
