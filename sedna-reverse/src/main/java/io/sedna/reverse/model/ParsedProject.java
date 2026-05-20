package io.sedna.reverse.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Parsed Spring project sources.
 *
 * @param projectRoot Gradle project root directory
 * @param classesByName parsed classes keyed by qualified name
 */
public record ParsedProject(Path projectRoot, Map<String, ParsedClass> classesByName) {

  /** Canonicalizes the class map ordering. */
  public ParsedProject {
    classesByName = Map.copyOf(new TreeMap<>(classesByName));
  }

  /**
   * Returns parsed classes in deterministic qualified-name order.
   *
   * @return immutable list of parsed classes
   */
  public List<ParsedClass> classes() {
    return List.copyOf(classesByName.values());
  }
}
