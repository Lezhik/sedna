package io.sedna.runtime.compensation;

import io.sedna.core.ExecutionToken;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.execution.DeterministicTokenFactory;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import io.sedna.runtime.trace.TraceEventKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** SUPERVISOR compensation in reverse canonical execution order. */
public final class OrderedCompensationHandler implements CompensationHandler {

  @Override
  public Result<Boolean, SemanticError> compensate(ExecutionToken failedToken) {
    return Result.ok(Boolean.TRUE);
  }

  public List<ExecutionTraceEvent> compensateExecutedNodes(
      long failedNodeId,
      List<ExecutionTraceEvent> executedEvents,
      RuntimeExecutionPlan plan,
      long startingSequence) {
    SemanticGraph graph = plan.graph();
    List<ExecutionTraceEvent> executeEvents =
        executedEvents.stream()
            .filter(event -> event.kind() == TraceEventKind.EXECUTE)
            .sorted(Comparator.comparingLong(ExecutionTraceEvent::sequenceNumber).reversed())
            .toList();

    List<ExecutionTraceEvent> compensationEvents = new ArrayList<>();
    long sequence = startingSequence;
    for (ExecutionTraceEvent executed : executeEvents) {
      if (executed.nodeId() == failedNodeId) {
        continue;
      }
      GenomeNode node = findNode(graph, executed.nodeId());
      sequence++;
      compensationEvents.add(
          new ExecutionTraceEvent(
              sequence,
              node.nodeId(),
              DeterministicTokenFactory.compensationToken(node, sequence, failedNodeId),
              TraceEventKind.COMPENSATE));
    }
    return compensationEvents;
  }

  private static GenomeNode findNode(SemanticGraph graph, long nodeId) {
    return graph.nodes().stream()
        .filter(node -> node.nodeId() == nodeId)
        .findFirst()
        .orElseThrow();
  }
}
