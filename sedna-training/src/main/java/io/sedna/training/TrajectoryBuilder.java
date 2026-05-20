package io.sedna.training;

import io.sedna.core.ExecutionOrdering;
import io.sedna.core.SemanticGraph;
import io.sedna.training.model.SemanticDelta;
import io.sedna.training.model.SemanticSnapshot;
import io.sedna.training.model.SemanticTrajectory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds trajectories from ordered commit metadata and graph snapshots. */
public final class TrajectoryBuilder {

  private final SemanticDeltaExtractor deltaExtractor = new SemanticDeltaExtractor();

  /** Creates a trajectory builder. */
  public TrajectoryBuilder() {}

  /**
   * Builds a semantic trajectory from commit order and snapshots.
   *
   * @param projectPath project root path
   * @param commitOrder Git commit hashes oldest-first
   * @param snapshots semantic snapshots per commit
   * @return semantic trajectory with topology-ordered deltas
   */
  public SemanticTrajectory build(
      Path projectPath, List<String> commitOrder, List<SemanticSnapshot> snapshots) {
    List<SemanticDelta> deltas = new ArrayList<>();
    if (snapshots.size() >= 2) {
      for (int i = 1; i < snapshots.size(); i++) {
        SemanticSnapshot previous = snapshots.get(i - 1);
        SemanticSnapshot current = snapshots.get(i);
        String commitHash =
            commitOrder.isEmpty() ? current.commitHash() : commitOrder.get(Math.min(i, commitOrder.size() - 1));
        deltas.addAll(
            deltaExtractor.extract(commitHash, previous.graph(), current.graph()));
      }
    }
    if (!snapshots.isEmpty()) {
      deltas = orderDeltas(deltas, snapshots.getLast().graph());
    }
    return new SemanticTrajectory(
        projectPath.toAbsolutePath().normalize().toString(),
        commitOrder,
        snapshots,
        deltas);
  }

  private static List<SemanticDelta> orderDeltas(List<SemanticDelta> deltas, SemanticGraph graph) {
    var ordered = ExecutionOrdering.topologicalOrder(graph);
    List<Long> executionOrder = ordered.isOk() ? ordered.value() : List.of();
    Comparator<SemanticDelta> byTopology =
        Comparator.comparingInt((SemanticDelta delta) -> topologyRank(delta.nodeId(), executionOrder))
            .thenComparingLong(SemanticDelta::nodeId)
            .thenComparing(SemanticDelta::deltaKind)
            .thenComparing(SemanticDelta::payload);
    return deltas.stream().sorted(byTopology).toList();
  }

  private static int topologyRank(long nodeId, List<Long> executionOrder) {
    int index = executionOrder.indexOf(nodeId);
    return index < 0 ? Integer.MAX_VALUE : index;
  }
}
