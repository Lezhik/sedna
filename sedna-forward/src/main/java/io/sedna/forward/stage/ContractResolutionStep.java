package io.sedna.forward.stage;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.CapabilityRef;
import io.sedna.core.Contract;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.forward.model.BoundExecutionGraph;
import io.sedna.forward.model.MaterializedEdge;
import io.sedna.forward.model.SemanticHypergraph;
import io.sedna.forward.util.CapabilityVersionMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ContractResolutionStep {

  public Result<BoundExecutionGraph, SemanticError> resolve(SemanticHypergraph hypergraph) {
    SemanticGraph graph = hypergraph.graph();
    Map<String, List<ProviderBinding>> providersByCapability = indexProviders(graph);

    List<MaterializedEdge> edges = new ArrayList<>();
    for (GenomeNode node : CanonicalOrdering.sortNodes(graph.nodes())) {
      for (Contract contract : node.contracts()) {
        for (CapabilityRef required : contract.requires()) {
          ProviderBinding provider = findProvider(providersByCapability, required);
          if (provider == null) {
            return Result.err(
                new SemanticError(
                    ErrorCode.CONTRACT_UNRESOLVED,
                    node.nodeId(),
                    "No provider for capability " + required.canonical()));
          }
          edges.add(new MaterializedEdge(node.nodeId(), provider.nodeId(), required));
        }
      }
    }

    edges.sort(
        Comparator.comparingLong(MaterializedEdge::consumerNodeId)
            .thenComparingLong(MaterializedEdge::providerNodeId)
            .thenComparing(edge -> edge.capability().canonical()));

    return Result.ok(new BoundExecutionGraph(graph, edges));
  }

  private static Map<String, List<ProviderBinding>> indexProviders(SemanticGraph graph) {
    Map<String, List<ProviderBinding>> index = new TreeMap<>();
    for (GenomeNode node : CanonicalOrdering.sortNodes(graph.nodes())) {
      for (Contract contract : node.contracts()) {
        for (CapabilityRef provides : contract.provides()) {
          index.computeIfAbsent(provides.name(), ignored -> new ArrayList<>())
              .add(new ProviderBinding(node.nodeId(), provides.versionConstraint()));
        }
      }
    }
    for (List<ProviderBinding> bindings : index.values()) {
      bindings.sort(Comparator.comparingLong(ProviderBinding::nodeId));
    }
    return index;
  }

  private static ProviderBinding findProvider(
      Map<String, List<ProviderBinding>> providersByCapability, CapabilityRef required) {
    List<ProviderBinding> providers = providersByCapability.get(required.name());
    if (providers == null) {
      return null;
    }
    for (ProviderBinding provider : providers) {
      if (CapabilityVersionMatcher.satisfies(
          required.versionConstraint(), provider.versionConstraint())) {
        return provider;
      }
    }
    return null;
  }

  private record ProviderBinding(long nodeId, String versionConstraint) {}
}
