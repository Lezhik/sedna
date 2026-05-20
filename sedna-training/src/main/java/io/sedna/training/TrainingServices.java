package io.sedna.training;

import io.sedna.dna.DnaServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;

/** Factory for default {@link TrainingPipeline} instances. */
public final class TrainingServices {

  private TrainingServices() {}

  /**
   * Returns a training pipeline bootstrapped with the embedded core registry.
   *
   * @return configured training pipeline
   */
  public static TrainingPipeline pipeline() {
    var registry = InMemorySemanticRegistry.bootstrap();
    return new TrainingPipeline(ReverseServices.pipeline(registry), DnaServices.encoder(), registry);
  }
}
