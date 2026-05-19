package io.sedna.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistryVersionCompatibilityTest {

  @Test
  void exactMatchIsCompatible() {
    var v = new RegistryVersion("core", 1, 0);
    assertTrue(RegistryVersionCompatibility.isCompatible(v, v));
  }

  @Test
  void lowerGraphMinorIsCompatible() {
    var graph = new RegistryVersion("core", 1, 0);
    var registry = new RegistryVersion("core", 1, 1);
    assertTrue(RegistryVersionCompatibility.isCompatible(graph, registry));
  }

  @Test
  void higherGraphMinorIsIncompatible() {
    var graph = new RegistryVersion("core", 1, 2);
    var registry = new RegistryVersion("core", 1, 1);
    assertFalse(RegistryVersionCompatibility.isCompatible(graph, registry));
  }

  @Test
  void differentMajorIsIncompatible() {
    var graph = new RegistryVersion("core", 2, 0);
    var registry = new RegistryVersion("core", 1, 0);
    assertFalse(RegistryVersionCompatibility.isCompatible(graph, registry));
  }

  @Test
  void differentVocabularyIdIsIncompatible() {
    var graph = new RegistryVersion("core", 1, 0);
    var registry = new RegistryVersion("ext", 1, 0);
    assertFalse(RegistryVersionCompatibility.isCompatible(graph, registry));
  }
}
