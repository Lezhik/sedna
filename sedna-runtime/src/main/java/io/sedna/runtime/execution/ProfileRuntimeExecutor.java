package io.sedna.runtime.execution;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.compensation.OrderedCompensationHandler;
import io.sedna.runtime.plan.RuntimeExecutionPlan;
import io.sedna.runtime.state.FsmStateTracker;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import io.sedna.runtime.trace.TraceEventKind;
import java.util.ArrayList;
import java.util.List;

/** Profile-aware deterministic runtime executor (DAG, STATEFUL, SUPERVISOR). */
public final class ProfileRuntimeExecutor {

  private final OrderedCompensationHandler compensationHandler = new OrderedCompensationHandler();

  public Result<ExecutionTrace, SemanticError> execute(
      RuntimeExecutionPlan plan, RuntimeExecutionOptions options) {
    return switch (plan.profile()) {
      case DAG -> executeDag(plan, options);
      case STATEFUL -> executeStateful(plan, options);
      case SUPERVISOR -> executeSupervisor(plan, options);
    };
  }

  private Result<ExecutionTrace, SemanticError> executeDag(
      RuntimeExecutionPlan plan, RuntimeExecutionOptions options) {
    List<ExecutionTraceEvent> events = new ArrayList<>();
    SemanticGraph graph = plan.graph();
    int startIndex = options.resumeAfterNodes();
    if (startIndex >= plan.orderedNodeIds().size()) {
      return Result.ok(new ExecutionTrace(events));
    }

    long sequence = startIndex;
    for (int index = startIndex; index < plan.orderedNodeIds().size(); index++) {
      long nodeId = plan.orderedNodeIds().get(index);
      GenomeNode node = findNode(graph, nodeId);
      sequence++;
      events.add(
          new ExecutionTraceEvent(
              sequence, nodeId, DeterministicTokenFactory.token(node, sequence)));
    }
    return Result.ok(new ExecutionTrace(events));
  }

  private Result<ExecutionTrace, SemanticError> executeStateful(
      RuntimeExecutionPlan plan, RuntimeExecutionOptions options) {
    List<ExecutionTraceEvent> events = new ArrayList<>();
    SemanticGraph graph = plan.graph();
    FsmStateTracker fsm = new FsmStateTracker();
    fsm.restore(options.resumeFsmState());

    int startIndex = options.resumeAfterNodes();
    long sequence = startIndex * 2L;
    for (int index = startIndex; index < plan.orderedNodeIds().size(); index++) {
      long nodeId = plan.orderedNodeIds().get(index);
      GenomeNode node = findNode(graph, nodeId);
      fsm.enterNode(nodeId, node.kind());
      events.add(
          new ExecutionTraceEvent(
              sequence,
              nodeId,
              DeterministicTokenFactory.statefulToken(node, sequence, fsm.stateHash()),
              TraceEventKind.STATE_TRANSITION));
      sequence++;
      fsm.completeNode(nodeId);
      events.add(
          new ExecutionTraceEvent(
              sequence,
              nodeId,
              DeterministicTokenFactory.statefulToken(node, sequence, fsm.stateHash()),
              TraceEventKind.EXECUTE));
    }
    return Result.ok(new ExecutionTrace(events));
  }

  private Result<ExecutionTrace, SemanticError> executeSupervisor(
      RuntimeExecutionPlan plan, RuntimeExecutionOptions options) {
    List<ExecutionTraceEvent> events = new ArrayList<>();
    SemanticGraph graph = plan.graph();
    long sequence = 0L;
    long failureNode = options.injectFailureAfterNodeId();

    for (long nodeId : plan.orderedNodeIds()) {
      GenomeNode node = findNode(graph, nodeId);
      sequence++;
      ExecutionTraceEvent executed =
          new ExecutionTraceEvent(
              sequence, nodeId, DeterministicTokenFactory.token(node, sequence));
      events.add(executed);

      if (failureNode != 0L && nodeId == failureNode) {
        List<ExecutionTraceEvent> compensation =
            compensationHandler.compensateExecutedNodes(failureNode, events, plan, sequence);
        events.addAll(compensation);
        return Result.ok(new ExecutionTrace(events));
      }
    }
    return Result.ok(new ExecutionTrace(events));
  }

  public String fsmStateAfter(ExecutionTrace trace, RuntimeExecutionPlan plan) {
    if (plan.profile() != ExecutionProfile.STATEFUL) {
      return "INIT";
    }
    FsmStateTracker tracker = new FsmStateTracker();
    SemanticGraph graph = plan.graph();
    for (ExecutionTraceEvent event : trace.events()) {
      if (event.kind() == TraceEventKind.STATE_TRANSITION) {
        GenomeNode node = findNode(graph, event.nodeId());
        tracker.enterNode(event.nodeId(), node.kind());
      } else if (event.kind() == TraceEventKind.EXECUTE) {
        tracker.completeNode(event.nodeId());
      }
    }
    return tracker.state();
  }

  private static GenomeNode findNode(SemanticGraph graph, long nodeId) {
    return graph.nodes().stream()
        .filter(node -> node.nodeId() == nodeId)
        .findFirst()
        .orElseThrow();
  }
}
