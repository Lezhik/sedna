package io.sedna.mutation;

import io.sedna.dna.DnaServices;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.validation.CompositeValidationEngine;
import io.sedna.validation.DnaRoundTripCodegenProbe;
import io.sedna.validation.MutationSafetyEngine;

public final class MutationServices {

  private MutationServices() {}

  public static MutationEngine engine() {
    var registry = InMemorySemanticRegistry.bootstrap();
    return new DefaultMutationEngine(
        SednaFoldMotifCodec.INSTANCE,
        SednaFoldMotifCodec.INSTANCE,
        new MutationSafetyEngine(),
        CompositeValidationEngine.standard(registry),
        new DnaRoundTripCodegenProbe(DnaServices.encoder(), DnaServices.decoder()));
  }
}
