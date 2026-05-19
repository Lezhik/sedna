package io.sedna.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForwardDeterminismTest {

  @Test
  void tenRunsProduceIdenticalProjectHash() {
    byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    ForwardPipeline pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);

    String firstHash = null;
    for (int i = 0; i < 10; i++) {
      var result = pipeline.run(dna);
      assertTrue(result.isOk(), () -> String.valueOf(result.error()));
      String hash = projectHash(result.value().files());
      if (firstHash == null) {
        firstHash = hash;
      } else {
        assertEquals(firstHash, hash, "Run " + i + " diverged");
      }
    }
  }

  private static String projectHash(Map<String, String> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (Map.Entry<String, String> entry : files.entrySet()) {
        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
