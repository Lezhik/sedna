package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.examples.ExamplesLayout;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.forward.ForwardPipeline;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** CI determinism gates: DNA byte identity and forward output hash (LLM off). */
class CiDeterminismTest {

  private static final String GOLDEN_SHA256 =
      "6d75d8431baaac07398e39448b19db34b62cc6df7eb84cbdc1484d5c2d7ed8f5";

  @Test
  void encodeDecodeBytesAreStable() {
    var graph = CmsReferenceFixtureGraph.create();
    byte[] encoded = DnaServices.encoder().encode(graph).value();
    byte[] roundTrip = DnaServices.encoder().encode(DnaServices.decoder().decode(encoded).value()).value();
    assertArrayEquals(encoded, roundTrip);
  }

  @Test
  void goldenFixtureFileMatchesCanonicalEncoding() throws Exception {
    Path cwd = Path.of("").toAbsolutePath();
    Path fixture = ExamplesLayout.goldenCmsFixture(cwd);
    if (!Files.isRegularFile(fixture)) {
      fixture = ExamplesLayout.goldenCmsFixture(cwd.resolve("..").normalize());
    }
    assertTrue(Files.isRegularFile(fixture), "Missing golden fixture: " + fixture);
    byte[] onDisk = Files.readAllBytes(fixture);
    byte[] generated =
        DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    assertArrayEquals(generated, onDisk);
    assertEquals(GOLDEN_SHA256, sha256(onDisk));
  }

  @Test
  void forwardOutputTreeHashIsStable() {
    byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    ForwardPipeline pipeline =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var first = pipeline.run(dna);
    var second = pipeline.run(dna);
    assertTrue(first.isOk());
    assertTrue(second.isOk());
    assertEquals(projectHash(first.value().files()), projectHash(second.value().files()));
  }

  private static String projectHash(Map<String, String> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      TreeMap<String, String> ordered = new TreeMap<>(files);
      for (Map.Entry<String, String> entry : ordered.entrySet()) {
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

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(bytes);
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
