package io.sedna.registry;

import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import java.util.Map;

/**
 * Decodes registry extension TLV payloads (bootstrap step 3). MVP: empty extensions until
 * extension format is finalized.
 */
public interface RegistryExtensionDecoder {

  Result<Map<String, SemanticDefinition>, SemanticError> decode(byte[] extensionPayload);
}
