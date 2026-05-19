package io.sedna.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.VocabRef;
import org.junit.jupiter.api.Test;

class InMemorySemanticRegistryTest {

  @Test
  void resolveKnownVocabRef() {
    SemanticRegistry registry = InMemorySemanticRegistry.bootstrap();
    VocabRef ref = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
    Result<SemanticDefinition, ?> result = registry.resolve(ref);
    assertTrue(result.isOk());
    assertEquals(ref, result.value().ref());
  }

  @Test
  void unknownVocabRefReturnsError() {
    SemanticRegistry registry = InMemorySemanticRegistry.bootstrap();
    VocabRef ref = new VocabRef("core", "UNKNOWN.TERM", "v1");
    Result<SemanticDefinition, io.sedna.core.SemanticError> result = registry.resolve(ref);
    assertFalse(result.isOk());
    assertEquals(ErrorCode.UNKNOWN_VOCAB, result.error().code());
  }
}
