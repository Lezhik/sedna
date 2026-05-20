package io.sedna.reverse.unknown;

import io.sedna.core.Constraint;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import io.sedna.dna.NodeIdHasher;
import io.sedna.reverse.cms.CmsSemanticRules;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.StructuralGraph;
import io.sedna.reverse.spring.SpringBootSemanticRules;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adds UNKNOWN-classified nodes for unmapped types (topology of core graph unchanged). */
public final class UnknownNodeEnrichmentStep {

  public static final String UNKNOWN_LABEL_PREFIX = "UNKNOWN_LABEL:";
  public static final String LLM_LABEL_PREFIX = "LLM_LABEL:";

  private static final VocabRef UNKNOWN =
      new VocabRef("core", "SEMANTIC.UNKNOWN", "v1");

  public Result<SemanticGraph, SemanticError> enrich(
      StructuralGraph structural, SemanticGraph graph, UnknownLabelProvider labelProvider) {
    Set<String> mapped = mappedQualifiedNames(structural, graph);
    List<GenomeNode> nodes = new ArrayList<>(graph.nodes());
    List<SemanticLink> links = new ArrayList<>(graph.links());

    List<ParsedClass> unknownClasses =
        structural.project().classes().stream()
            .filter(parsed -> !mapped.contains(parsed.qualifiedName()))
            .filter(parsed -> !isInfrastructure(parsed))
            .sorted((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()))
            .toList();

    for (ParsedClass parsed : unknownClasses) {
      String label = labelProvider.labelFor(parsed);
      Constraint labelConstraint = new Constraint(UNKNOWN_LABEL_PREFIX + label);
      Constraint classConstraint = new Constraint("SOURCE_CLASS:" + parsed.qualifiedName());
      VocabRef instanceRef =
          new VocabRef("core", "UNKNOWN.INSTANCE." + sanitize(parsed.qualifiedName()), "v1");
      GenomeNode node =
          NodeIdHasher.withCanonicalNodeId(
              new GenomeNode(
                  0L,
                  NodeKind.INTEGRATION,
                  new io.sedna.core.SemanticCore(UNKNOWN, UNKNOWN, UNKNOWN, List.of(instanceRef)),
                  List.of(),
                  List.of(labelConstraint, classConstraint)));
      nodes.add(node);
    }

    return Result.ok(new SemanticGraph(nodes, links, graph.vocabularyVersion()));
  }

  private static String sanitize(String qualified) {
    return qualified.replace('.', '_');
  }

  private static boolean isInfrastructure(ParsedClass parsed) {
    String qualified = parsed.qualifiedName();
    return qualified.endsWith("package-info")
        || parsed.simpleName().endsWith("Application");
  }

  private static Set<String> mappedQualifiedNames(StructuralGraph structural, SemanticGraph graph) {
    Set<String> mapped = new HashSet<>();
    for (ParsedClass parsed : structural.project().classes()) {
      if (CmsSemanticRules.classify(parsed).isPresent()
          || SpringBootSemanticRules.classify(parsed).isPresent()) {
        mapped.add(parsed.qualifiedName());
      }
    }
    for (GenomeNode node : graph.nodes()) {
      for (Constraint constraint : node.constraints()) {
        if (constraint.code().startsWith("SOURCE_CLASS:")) {
          mapped.add(constraint.code().substring("SOURCE_CLASS:".length()));
        }
      }
      for (var contract : node.contracts()) {
        String payload = contract.ioSchema().payload();
        if (payload.startsWith("class:")) {
          mapped.add(payload.substring("class:".length()));
        }
      }
    }
    return mapped;
  }

  /** Optional LLM or heuristic label provider. */
  public interface UnknownLabelProvider {
    String labelFor(ParsedClass parsed);

    static UnknownLabelProvider heuristic() {
      return parsed -> parsed.simpleName();
    }

    static UnknownLabelProvider withLlmFallback(UnknownLabelProvider delegate) {
      return delegate;
    }
  }
}
