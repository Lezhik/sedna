package io.sedna.reverse;

import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.registry.SemanticRegistry;

/** Factory for default {@link ReversePipeline} instances. */
public final class ReverseServices {

  private ReverseServices() {}

  /**
   * Returns a pipeline bootstrapped with the embedded core registry.
   *
   * @return configured reverse pipeline
   */
  public static ReversePipeline pipeline() {
    return pipeline(InMemorySemanticRegistry.bootstrap());
  }

  /**
   * Returns a pipeline using the supplied registry for validation.
   *
   * @param registry semantic registry
   * @return configured reverse pipeline
   */
  public static ReversePipeline pipeline(SemanticRegistry registry) {
    DnaEncoder encoder = DnaServices.encoder();
    return ReversePipeline.standard(encoder, registry);
  }
}
