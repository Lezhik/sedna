package io.sedna.registry;

import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import java.util.Map;

/** Delegates to {@link TlvRegistryExtensionDecoder}. */
public final class EmptyRegistryExtensionDecoder implements RegistryExtensionDecoder {

  private final RegistryExtensionDecoder delegate = new TlvRegistryExtensionDecoder();

  @Override
  public Result<Map<String, SemanticDefinition>, SemanticError> decode(byte[] extensionPayload) {
    return delegate.decode(extensionPayload);
  }
}
