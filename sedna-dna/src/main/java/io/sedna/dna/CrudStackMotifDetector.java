package io.sedna.dna;

import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.NodeKind;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Deterministic CRUD_STACK motif detection (Controller → Service → Entity). */
final class CrudStackMotifDetector {

  record Match(
      GenomeNode controller,
      GenomeNode service,
      GenomeNode entity,
      SemanticLink controllerToService,
      SemanticLink serviceToEntity,
      boolean partialMatch) {}

  private CrudStackMotifDetector() {}

  static Optional<Match> find(SemanticGraph graph) {
    if (graph.nodes().stream().anyMatch(node -> node.kind() == NodeKind.MOTIF)) {
      return Optional.empty();
    }
    Map<Long, GenomeNode> nodesById = new TreeMap<>();
    for (GenomeNode node : graph.nodes()) {
      nodesById.put(node.nodeId(), node);
    }
    List<GenomeNode> controllers =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.CONTROLLER)
            .sorted(Comparator.comparingLong(GenomeNode::nodeId))
            .toList();

    for (GenomeNode controller : controllers) {
      Optional<Match> match = matchFromController(graph, nodesById, controller);
      if (match.isPresent()) {
        return match;
      }
    }
    return Optional.empty();
  }

  private static Optional<Match> matchFromController(
      SemanticGraph graph, Map<Long, GenomeNode> nodesById, GenomeNode controller) {
    List<SemanticLink> outgoing =
        graph.links().stream()
            .filter(link -> link.sourceNodeId() == controller.nodeId())
            .filter(link -> link.type() == LinkType.DEPENDENCY)
            .sorted(
                Comparator.comparingLong(SemanticLink::targetNodeId)
                    .thenComparing(link -> link.type().name()))
            .toList();
    for (SemanticLink controllerToService : outgoing) {
      GenomeNode service = nodesById.get(controllerToService.targetNodeId());
      if (service == null || service.kind() != NodeKind.SERVICE) {
        continue;
      }
      Optional<SemanticLink> serviceToEntity =
          graph.links().stream()
              .filter(link -> link.sourceNodeId() == service.nodeId())
              .filter(link -> link.type() == LinkType.DEPENDENCY)
              .sorted(Comparator.comparingLong(SemanticLink::targetNodeId))
              .findFirst();
      if (serviceToEntity.isEmpty()) {
        continue;
      }
      GenomeNode entity = nodesById.get(serviceToEntity.get().targetNodeId());
      if (entity == null || entity.kind() != NodeKind.ENTITY) {
        continue;
      }
      Set<Long> motifNodes =
          Set.of(controller.nodeId(), service.nodeId(), entity.nodeId());
      if (!coversEligibleNodes(graph, motifNodes)) {
        continue;
      }
      boolean partial = isPartialMatch(controller, service, entity);
      return Optional.of(
          new Match(
              controller, service, entity, controllerToService, serviceToEntity.get(), partial));
    }
    return Optional.empty();
  }

  private static boolean coversEligibleNodes(SemanticGraph graph, Set<Long> motifNodes) {
    for (GenomeNode node : graph.nodes()) {
      if (node.kind() == NodeKind.MOTIF) {
        return false;
      }
      if (!motifNodes.contains(node.nodeId())
          && node.kind() != NodeKind.CONTROLLER
          && node.kind() != NodeKind.SERVICE
          && node.kind() != NodeKind.ENTITY) {
        return false;
      }
    }
    return motifNodes.size() == 3;
  }

  private static boolean isPartialMatch(GenomeNode controller, GenomeNode service, GenomeNode entity) {
    return hasNonStandardConstraints(controller)
        || hasNonStandardConstraints(service)
        || hasNonStandardConstraints(entity)
        || service.contracts().isEmpty()
        || entity.contracts().isEmpty();
  }

  private static boolean hasNonStandardConstraints(GenomeNode node) {
    for (var constraint : node.constraints()) {
      String code = constraint.code();
      if (code.startsWith(SednaFoldV1.MOTIF_REF_PREFIX)
          || code.startsWith(SednaFoldV1.FOLD_PAYLOAD_PREFIX)
          || code.equals(SednaFoldV1.PARTIAL_MATCH_FLAG)) {
        continue;
      }
      if (code.startsWith("SOURCE_PACKAGE:")
          || code.startsWith("SOURCE_CLASS:")
          || code.equals("STATELESS_ONLY")
          || code.equals("TRANSACTIONAL")
          || code.equals("READ_ONLY")) {
        continue;
      }
      return true;
    }
    return false;
  }
}
