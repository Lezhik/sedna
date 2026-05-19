package io.sedna.dna;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Constraint;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.Result;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** SEDNA-FOLD-v1 motif fold/expand codec (replaces identity pass-through). */
public final class SednaFoldMotifCodec implements MotifFolder, MotifExpander {

  public static final SednaFoldMotifCodec INSTANCE = new SednaFoldMotifCodec();

  private static final VocabRef MOTIF_VOCAB = new VocabRef("core", "MOTIF.CRUD_STACK", "v1");

  private SednaFoldMotifCodec() {}

  @Override
  public Result<SemanticGraph, SemanticError> fold(SemanticGraph graph) {
    Optional<CrudStackMotifDetector.Match> match = CrudStackMotifDetector.find(graph);
    if (match.isEmpty()) {
      return Result.ok(graph);
    }
    return foldCrudStack(graph, match.get());
  }

  @Override
  public Result<SemanticGraph, SemanticError> expand(SemanticGraph graph) {
    List<GenomeNode> motifNodes =
        graph.nodes().stream()
            .filter(node -> node.kind() == NodeKind.MOTIF)
            .sorted((a, b) -> Long.compare(a.nodeId(), b.nodeId()))
            .toList();
    if (motifNodes.isEmpty()) {
      return Result.ok(graph);
    }
    try {
      java.util.Map<Long, Long> motifAnchors = new java.util.TreeMap<>();
      List<GenomeNode> nodes = new ArrayList<>();
      List<SemanticLink> links = new ArrayList<>();

      for (GenomeNode node : graph.nodes()) {
        if (node.kind() != NodeKind.MOTIF) {
          nodes.add(node);
        }
      }

      for (GenomeNode motif : motifNodes) {
        FoldPayloadCodec.Payload payload = decodeMotifPayload(motif);
        motifAnchors.put(motif.nodeId(), payload.anchorNodeId());
        nodes.addAll(payload.members());
        links.addAll(payload.links());
      }

      for (SemanticLink link : graph.links()) {
        Long anchorSource = motifAnchors.get(link.sourceNodeId());
        Long anchorTarget = motifAnchors.get(link.targetNodeId());
        if (anchorSource == null && anchorTarget == null) {
          links.add(link);
        } else if (anchorSource != null && anchorTarget != null) {
          continue;
        } else if (anchorSource != null) {
          links.add(new SemanticLink(anchorSource, link.targetNodeId(), link.type()));
        } else {
          links.add(new SemanticLink(link.sourceNodeId(), anchorTarget, link.type()));
        }
      }

      return Result.ok(
          CanonicalOrdering.canonicalize(
              new SemanticGraph(nodes, links, graph.vocabularyVersion())));
    } catch (IllegalArgumentException ex) {
      return Result.err(SemanticError.global(ErrorCode.INVALID_DNA, ex.getMessage()));
    }
  }

  private static Result<SemanticGraph, SemanticError> foldCrudStack(
      SemanticGraph graph, CrudStackMotifDetector.Match match) {
    Set<Long> foldedIds =
        Set.of(
            match.controller().nodeId(),
            match.service().nodeId(),
            match.entity().nodeId());

    List<GenomeNode> members =
        List.of(match.controller(), match.service(), match.entity());
    List<SemanticLink> internalLinks =
        List.of(match.controllerToService(), match.serviceToEntity());

    FoldPayloadCodec.Payload payload =
        new FoldPayloadCodec.Payload(
            SednaFoldV1.MOTIF_CRUD_STACK,
            match.partialMatch(),
            match.controller().nodeId(),
            members,
            internalLinks);

    List<Constraint> motifConstraints = new ArrayList<>();
    motifConstraints.add(new Constraint(SednaFoldV1.MOTIF_REF_PREFIX + SednaFoldV1.MOTIF_CRUD_STACK));
    motifConstraints.add(new Constraint(FoldPayloadCodec.encodeToConstraint(payload)));
    if (match.partialMatch()) {
      motifConstraints.add(new Constraint(SednaFoldV1.PARTIAL_MATCH_FLAG));
    }

    SemanticCore motifCore = new SemanticCore(MOTIF_VOCAB, MOTIF_VOCAB, MOTIF_VOCAB, List.of());
    GenomeNode motifNode =
        NodeIdHasher.withCanonicalNodeId(
            new GenomeNode(0L, NodeKind.MOTIF, motifCore, List.of(), motifConstraints));

    List<GenomeNode> nodes = new ArrayList<>();
    for (GenomeNode node : graph.nodes()) {
      if (!foldedIds.contains(node.nodeId())) {
        nodes.add(node);
      }
    }
    nodes.add(motifNode);

    List<SemanticLink> links = new ArrayList<>();
    long motifId = motifNode.nodeId();
    for (SemanticLink link : graph.links()) {
      long source = link.sourceNodeId();
      long target = link.targetNodeId();
      boolean sourceFolded = foldedIds.contains(source);
      boolean targetFolded = foldedIds.contains(target);
      if (sourceFolded && targetFolded) {
        continue;
      }
      if (sourceFolded) {
        links.add(new SemanticLink(motifId, target, link.type()));
      } else if (targetFolded) {
        links.add(new SemanticLink(source, motifId, link.type()));
      } else {
        links.add(link);
      }
    }

    return Result.ok(
        CanonicalOrdering.canonicalize(
            new SemanticGraph(nodes, links, graph.vocabularyVersion())));
  }

  public static FoldPayloadCodec.Payload decodeMotifPayload(GenomeNode motif) {
    for (Constraint constraint : motif.constraints()) {
      if (constraint.code().startsWith(SednaFoldV1.FOLD_PAYLOAD_PREFIX)) {
        return FoldPayloadCodec.decodeFromConstraint(constraint.code());
      }
    }
    throw new IllegalArgumentException("Missing SEDNA_FOLD_V1 payload on motif node " + motif.nodeId());
  }
}
