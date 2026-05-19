package io.sedna.mutation;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Constraint;
import io.sedna.core.Contract;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.Mutation;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.dna.MotifExpander;
import io.sedna.dna.MotifFolder;
import io.sedna.dna.NodeIdHasher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Applies a single mutation operation to a graph copy. */
public final class MutationApplicator {

  private final MotifFolder motifFolder;
  private final MotifExpander motifExpander;

  public MutationApplicator(MotifFolder motifFolder, MotifExpander motifExpander) {
    this.motifFolder = motifFolder;
    this.motifExpander = motifExpander;
  }

  public Result<SemanticGraph, SemanticError> apply(SemanticGraph graph, Mutation mutation) {
    return switch (mutation.operation()) {
      case NODE_DELETE -> deleteNode(graph, mutation.targetNodeId());
      case NODE_INSERT -> insertNode(graph, mutation);
      case SUBTREE_REPLACE -> replaceSubtree(graph, mutation);
      case MOTIF_FOLD -> motifFolder.fold(graph);
      case MOTIF_EXPAND -> motifExpander.expand(graph);
      case CONTRACT_UPGRADE -> upgradeContract(graph, mutation);
      case CONSTRAINT_INJECTION -> injectConstraint(graph, mutation);
    };
  }

  private static Result<SemanticGraph, SemanticError> deleteNode(SemanticGraph graph, long targetNodeId) {
    Set<Long> remove = SubgraphIndex.subtreeNodeIds(graph, targetNodeId);
    List<GenomeNode> nodes =
        graph.nodes().stream().filter(node -> !remove.contains(node.nodeId())).toList();
    List<SemanticLink> links =
        graph.links().stream()
            .filter(
                link ->
                    !remove.contains(link.sourceNodeId()) && !remove.contains(link.targetNodeId()))
            .toList();
    if (nodes.isEmpty()) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Cannot delete entire graph"));
    }
    return Result.ok(new SemanticGraph(nodes, links, graph.vocabularyVersion()));
  }

  private static Result<SemanticGraph, SemanticError> insertNode(SemanticGraph graph, Mutation mutation) {
    GenomeNode raw = mutation.insertNode().orElse(null);
    if (raw == null) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "NODE_INSERT requires insertNode operand"));
    }
    GenomeNode inserted = NodeIdHasher.withCanonicalNodeId(raw);
    if (graph.nodes().stream().anyMatch(node -> node.nodeId() == inserted.nodeId())) {
      return Result.err(
          new SemanticError(
              ErrorCode.VALIDATION_FAILED, inserted.nodeId(), "Duplicate nodeId on insert"));
    }
    List<GenomeNode> nodes = new ArrayList<>(graph.nodes());
    nodes.add(inserted);
    List<SemanticLink> links = new ArrayList<>(graph.links());
    links.add(new SemanticLink(mutation.targetNodeId(), inserted.nodeId(), LinkType.DEPENDENCY));
    return Result.ok(new SemanticGraph(nodes, links, graph.vocabularyVersion()));
  }

  private static Result<SemanticGraph, SemanticError> replaceSubtree(SemanticGraph graph, Mutation mutation) {
    SemanticGraph replacement = mutation.replacementSubtree().orElse(null);
    if (replacement == null) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "SUBTREE_REPLACE requires replacementSubtree operand"));
    }
    Set<Long> remove = SubgraphIndex.subtreeNodeIds(graph, mutation.targetNodeId());
    List<GenomeNode> nodes =
        graph.nodes().stream().filter(node -> !remove.contains(node.nodeId())).toList();
    List<SemanticLink> links =
        graph.links().stream()
            .filter(
                link ->
                    !remove.contains(link.sourceNodeId()) && !remove.contains(link.targetNodeId()))
            .toList();

    List<GenomeNode> mergedNodes = new ArrayList<>(nodes);
    mergedNodes.addAll(replacement.nodes());
    List<SemanticLink> mergedLinks = new ArrayList<>(links);
    mergedLinks.addAll(replacement.links());
    return Result.ok(
        CanonicalOrdering.canonicalize(
            new SemanticGraph(mergedNodes, mergedLinks, graph.vocabularyVersion())));
  }

  private static Result<SemanticGraph, SemanticError> upgradeContract(SemanticGraph graph, Mutation mutation) {
    Contract upgrade = mutation.contractUpgrade().orElse(null);
    if (upgrade == null) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "CONTRACT_UPGRADE requires contractUpgrade operand"));
    }
    List<GenomeNode> nodes = new ArrayList<>();
    for (GenomeNode node : graph.nodes()) {
      if (node.nodeId() != mutation.targetNodeId()) {
        nodes.add(node);
        continue;
      }
      List<Contract> contracts = new ArrayList<>();
      boolean replaced = false;
      for (Contract contract : node.contracts()) {
        if (!contract.provides().isEmpty()
            && contract.provides().getFirst().name().equals(upgrade.provides().getFirst().name())) {
          contracts.add(upgrade);
          replaced = true;
        } else {
          contracts.add(contract);
        }
      }
      if (!replaced) {
        contracts.add(upgrade);
      }
      GenomeNode updated =
          NodeIdHasher.withCanonicalNodeId(
              new GenomeNode(0L, node.kind(), node.core(), contracts, node.constraints()));
      nodes.add(updated);
    }
    return Result.ok(new SemanticGraph(nodes, graph.links(), graph.vocabularyVersion()));
  }

  private static Result<SemanticGraph, SemanticError> injectConstraint(SemanticGraph graph, Mutation mutation) {
    Constraint constraint = mutation.injectedConstraint().orElse(null);
    if (constraint == null) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "CONSTRAINT_INJECTION requires injectedConstraint operand"));
    }
    List<GenomeNode> nodes = new ArrayList<>();
    for (GenomeNode node : graph.nodes()) {
      if (node.nodeId() != mutation.targetNodeId()) {
        nodes.add(node);
        continue;
      }
      Set<String> codes = new HashSet<>();
      List<Constraint> constraints = new ArrayList<>();
      for (Constraint existing : node.constraints()) {
        if (codes.add(existing.code())) {
          constraints.add(existing);
        }
      }
      if (codes.add(constraint.code())) {
        constraints.add(constraint);
      }
      nodes.add(
          new GenomeNode(node.nodeId(), node.kind(), node.core(), node.contracts(), constraints));
    }
    return Result.ok(new SemanticGraph(nodes, graph.links(), graph.vocabularyVersion()));
  }
}
