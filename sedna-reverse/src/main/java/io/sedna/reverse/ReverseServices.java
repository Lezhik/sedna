package io.sedna.reverse;

import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.registry.SemanticRegistry;

public final class ReverseServices {

  private ReverseServices() {}

  public static ReversePipeline pipeline() {
    return pipeline(InMemorySemanticRegistry.bootstrap());
  }

  public static ReversePipeline pipeline(SemanticRegistry registry) {
    DnaEncoder encoder = DnaServices.encoder();
    return ReversePipeline.standard(encoder, registry);
  }
}
