package io.sedna.registry;

import io.sedna.core.ErrorCode;
import io.sedna.core.RegistryVersion;
import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Bootstrap steps 1–3: core vocabulary + optional extension merge (deterministic). */
public final class RegistryBootstrap {

  private static final RegistryExtensionDecoder EXTENSION_DECODER = new TlvRegistryExtensionDecoder();

  private RegistryBootstrap() {}

  public static InMemorySemanticRegistry bootstrap() {
    return bootstrap(new byte[0]);
  }

  public static Result<InMemorySemanticRegistry, SemanticError> bootstrapResult(byte[] extensionPayload) {
    Map<String, SemanticDefinition> merged = new LinkedHashMap<>(EmbeddedCoreVocabulary.load());
    RegistryVersion version = EmbeddedCoreVocabulary.defaultVersion();

    byte[] payload = extensionPayload == null ? new byte[0] : extensionPayload;
    if (payload.length > 0) {
      Result<Map<String, SemanticDefinition>, SemanticError> decoded =
          EXTENSION_DECODER.decode(payload);
      if (!decoded.isOk()) {
        return Result.err(decoded.error());
      }
      TreeMap<String, SemanticDefinition> extensionEntries = new TreeMap<>(decoded.value());
      for (Map.Entry<String, SemanticDefinition> entry : extensionEntries.entrySet()) {
        if (merged.containsKey(entry.getKey())) {
          return Result.err(
              SemanticError.global(
                  ErrorCode.VALIDATION_FAILED,
                  "Extension vocabulary collides with core entry: " + entry.getKey()));
        }
        merged.put(entry.getKey(), entry.getValue());
      }
      version =
          new RegistryVersion(version.vocabularyId(), version.major(), version.minor() + 1);
    }
    return Result.ok(new InMemorySemanticRegistry(merged, version));
  }

  public static InMemorySemanticRegistry bootstrap(byte[] extensionPayload) {
    Result<InMemorySemanticRegistry, SemanticError> result = bootstrapResult(extensionPayload);
    if (!result.isOk()) {
      throw new IllegalStateException("Registry bootstrap failed: " + result.error().message());
    }
    return result.value();
  }
}
