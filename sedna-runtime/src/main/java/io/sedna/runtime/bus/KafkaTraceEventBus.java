package io.sedna.runtime.bus;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.trace.ExecutionTraceEvent;
import java.util.List;

/**
 * Kafka-backed trace bus placeholder (Phase 14). Configure {@code SEDNA_KAFKA_BOOTSTRAP_SERVERS} to
 * enable a real producer in future releases; until then returns structured NOT_IMPLEMENTED.
 */
public final class KafkaTraceEventBus implements TraceEventBus {

  private final String bootstrapServers;

  public KafkaTraceEventBus(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers == null ? "" : bootstrapServers.trim();
  }

  @Override
  public Result<Boolean, SemanticError> publish(ExecutionTraceEvent event) {
    if (bootstrapServers.isBlank()) {
      return Result.err(
          SemanticError.global(
              ErrorCode.NOT_IMPLEMENTED,
              "Kafka trace bus requires SEDNA_KAFKA_BOOTSTRAP_SERVERS"));
    }
    return Result.err(
        SemanticError.global(
            ErrorCode.NOT_IMPLEMENTED,
            "Kafka producer integration deferred; bootstrap=" + bootstrapServers));
  }

  @Override
  public Result<List<ExecutionTraceEvent>, SemanticError> drainOrdered() {
    return Result.err(
        SemanticError.global(ErrorCode.NOT_IMPLEMENTED, "Kafka consumer integration deferred"));
  }
}
