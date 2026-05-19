package io.sedna.registry;

import io.sedna.core.RegistryVersion;
import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import io.sedna.core.VocabRef;

/** Versioned semantic vocabulary resolution. */
public interface SemanticRegistry {

  Result<SemanticDefinition, SemanticError> resolve(VocabRef ref);

  RegistryVersion version();
}
