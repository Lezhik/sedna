package io.sedna.registry;

import io.sedna.core.ErrorCode;
import io.sedna.core.RegistryVersion;
import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import io.sedna.core.VocabRef;
import java.util.Map;

/** Immutable in-memory registry backed by embedded vocabulary. */
public final class InMemorySemanticRegistry implements SemanticRegistry {

  private final Map<String, SemanticDefinition> definitions;
  private final RegistryVersion version;

  public InMemorySemanticRegistry() {
    this(EmbeddedCoreVocabulary.load(), EmbeddedCoreVocabulary.defaultVersion());
  }

  public InMemorySemanticRegistry(Map<String, SemanticDefinition> definitions, RegistryVersion version) {
    this.definitions = Map.copyOf(definitions);
    this.version = version;
  }

  public static InMemorySemanticRegistry bootstrap() {
    return RegistryBootstrap.bootstrap();
  }

  @Override
  public Result<SemanticDefinition, SemanticError> resolve(VocabRef ref) {
    SemanticDefinition definition = definitions.get(ref.canonicalKey());
    if (definition == null) {
      return Result.err(
          new SemanticError(
              ErrorCode.UNKNOWN_VOCAB,
              0L,
              "Unknown vocabulary entry: " + ref.canonicalKey()));
    }
    return Result.ok(definition);
  }

  @Override
  public RegistryVersion version() {
    return version;
  }
}
