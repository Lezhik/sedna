package io.sedna.registry;

import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import java.util.Map;

/** MVP decoder: accepts empty payload only. */
public final class EmptyRegistryExtensionDecoder implements RegistryExtensionDecoder {

  @Override
  public Result<Map<String, SemanticDefinition>, SemanticError> decode(byte[] extensionPayload) {
    if (extensionPayload == null || extensionPayload.length == 0) {
      return Result.ok(Map.of());
    }
    return Result.err(
        io.sedna.core.SemanticError.global(
            io.sedna.core.ErrorCode.NOT_IMPLEMENTED,
            "Non-empty registry extensions not supported in MVP"));
  }
}
