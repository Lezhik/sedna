package io.sedna.runtime;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.runtime.plan.RuntimeExecutionPlan;

/** Builds canonical execution plans from semantic graphs. */
public interface RuntimeScheduler {

  Result<RuntimeExecutionPlan, SemanticError> build(SemanticGraph graph);
}
