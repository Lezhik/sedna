package io.sedna.registry;

import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import java.util.Map;

/** Decodes registry extension TLV payloads (bootstrap step 3, REG-EXT-v1). */
public interface RegistryExtensionDecoder {

  Result<Map<String, SemanticDefinition>, SemanticError> decode(byte[] extensionPayload);
}
