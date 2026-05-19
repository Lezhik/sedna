package io.sedna.forward.model;

import java.util.Map;
import java.util.TreeMap;

/** Deterministic path → UTF-8 text map (canonical path ordering). */
public final class GeneratedProject {

  private final TreeMap<String, String> files;

  public GeneratedProject(Map<String, String> files) {
    this.files = new TreeMap<>(files);
  }

  public Map<String, String> files() {
    return Map.copyOf(files);
  }
}
