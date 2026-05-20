package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-008 — forward output tree hash is stable (LLM off). */
@Tag("e2e")
class ForwardDeterminismE2eTest {

  @Test
  void forwardTreeHashMatchesAcrossRuns() {
    byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    var pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var first = pipeline.run(dna);
    var second = pipeline.run(dna);
    assertTrue(first.isOk());
    assertTrue(second.isOk());
    assertEquals(
        E2eTestSupport.treeHash(first.value().files()),
        E2eTestSupport.treeHash(second.value().files()));
  }
}
