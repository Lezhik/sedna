package io.sedna.forward;

import io.sedna.dna.DnaDecoder;
import io.sedna.dna.DnaServices;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.llm.OpenRouterLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.registry.SemanticRegistry;

public final class ForwardServices {

  private ForwardServices() {}

  public static ForwardPipeline pipeline() {
    return pipeline(InMemorySemanticRegistry.bootstrap(), OpenRouterLlmClient.createOrDisabled());
  }

  public static ForwardPipeline pipeline(SemanticRegistry registry, LlmClient llmClient) {
    DnaDecoder decoder = DnaServices.decoder();
    return ForwardPipeline.standard(decoder, registry, llmClient);
  }
}
