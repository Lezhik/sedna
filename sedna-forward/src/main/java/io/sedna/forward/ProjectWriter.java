package io.sedna.forward;

import io.sedna.forward.model.GeneratedProject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes generated files in canonical path order. */
public final class ProjectWriter {

  private ProjectWriter() {}

  public static void write(GeneratedProject project, Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);
    for (var entry : project.files().entrySet()) {
      Path target = outputDirectory.resolve(entry.getKey()).normalize();
      if (!target.startsWith(outputDirectory.normalize())) {
        throw new IOException("Path escapes output directory: " + entry.getKey());
      }
      var parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
    }
  }
}
