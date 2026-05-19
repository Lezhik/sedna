package io.sedna.registry;

import io.sedna.core.RegistryVersion;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.VocabRef;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
/** Loads embedded core vocabulary (bootstrap step 1). */
public final class EmbeddedCoreVocabulary {

  private static final String RESOURCE = "/vocabulary/core-vocabulary.csv";

  private EmbeddedCoreVocabulary() {}

  public static Map<String, SemanticDefinition> load() {
    Map<String, SemanticDefinition> definitions = new LinkedHashMap<>();
    try (InputStream in = EmbeddedCoreVocabulary.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource: " + RESOURCE);
      }
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        boolean headerSkipped = false;
        while ((line = reader.readLine()) != null) {
          if (line.isBlank() || line.startsWith("#")) {
            continue;
          }
          if (!headerSkipped) {
            headerSkipped = true;
            continue;
          }
          String[] parts = line.split(",", 4);
          if (parts.length < 4) {
            throw new IllegalStateException("Invalid vocabulary line: " + line);
          }
          VocabRef ref = new VocabRef(parts[0].trim(), parts[1].trim(), parts[2].trim());
          String displayName = parts[3].trim();
          definitions.put(ref.canonicalKey(), new SemanticDefinition(ref, displayName, displayName));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load core vocabulary", e);
    }
    return Map.copyOf(definitions);
  }

  public static RegistryVersion defaultVersion() {
    return new RegistryVersion("core", 1, 0);
  }
}
