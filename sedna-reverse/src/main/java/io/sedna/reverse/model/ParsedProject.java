package io.sedna.reverse.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Parsed Spring project sources. */
public record ParsedProject(Path projectRoot, Map<String, ParsedClass> classesByName) {
  public ParsedProject {
    classesByName = Map.copyOf(new TreeMap<>(classesByName));
  }

  public List<ParsedClass> classes() {
    return List.copyOf(classesByName.values());
  }
}
