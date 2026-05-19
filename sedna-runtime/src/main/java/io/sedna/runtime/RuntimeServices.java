package io.sedna.runtime;

import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.persistence.CheckpointStore;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.runtime.compensation.CompensationHandler;
import io.sedna.runtime.execution.DagRuntimeExecutor;
import io.sedna.runtime.replay.ReplayHarness;

public final class RuntimeServices {

  private RuntimeServices() {}

  public static RuntimeEngine engine() {
    return engine(new InMemoryCheckpointStore());
  }

  public static RuntimeEngine engine(CheckpointStore checkpointStore) {
    DefaultRuntimeScheduler scheduler = new DefaultRuntimeScheduler();
    DagRuntimeExecutor executor = new DagRuntimeExecutor(CompensationHandler.noOp());
    DnaEncoder encoder = DnaServices.encoder();
    return new RuntimeEngine(scheduler, executor, encoder, checkpointStore);
  }

  public static ReplayHarness replayHarness(CheckpointStore checkpointStore) {
    return new ReplayHarness(
        DnaServices.decoder(),
        new DefaultRuntimeScheduler(),
        new DagRuntimeExecutor(CompensationHandler.noOp()),
        checkpointStore);
  }
}
