package io.sedna.training;

import io.sedna.dna.DnaServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;

public final class TrainingServices {

  private TrainingServices() {}

  public static TrainingPipeline pipeline() {
    var registry = InMemorySemanticRegistry.bootstrap();
    return new TrainingPipeline(ReverseServices.pipeline(registry), DnaServices.encoder(), registry);
  }
}
