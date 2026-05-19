package io.sedna.core;

/**
 * Version pinning policy (FR-reg.03): same vocabulary id, same major, graph minor must not exceed
 * registry minor.
 */
public final class RegistryVersionCompatibility {

  private RegistryVersionCompatibility() {}

  public static boolean isCompatible(RegistryVersion graphVersion, RegistryVersion registryVersion) {
    if (!graphVersion.vocabularyId().equals(registryVersion.vocabularyId())) {
      return false;
    }
    if (graphVersion.major() != registryVersion.major()) {
      return false;
    }
    return graphVersion.minor() <= registryVersion.minor();
  }

  public static String incompatibilityReason(
      RegistryVersion graphVersion, RegistryVersion registryVersion) {
    if (isCompatible(graphVersion, registryVersion)) {
      return "";
    }
    return "Graph vocabularyVersion "
        + graphVersion.canonical()
        + " is not compatible with registry "
        + registryVersion.canonical()
        + " (policy: same major, graph.minor <= registry.minor)";
  }
}
