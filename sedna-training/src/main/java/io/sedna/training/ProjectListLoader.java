package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Loads project folder paths from a list file (one path per line; no repo-wide merge). */
public final class ProjectListLoader {

  public Result<List<Path>, SemanticError> load(Path projectsListFile) {
    if (!Files.isRegularFile(projectsListFile)) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "Projects list not found: " + projectsListFile));
    }
    try {
      TreeSet<String> ordered = new TreeSet<>();
      for (String line : Files.readAllLines(projectsListFile)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        ordered.add(trimmed);
      }
      List<Path> projects = new ArrayList<>();
      Path listParent = projectsListFile.getParent();
      for (String entry : ordered) {
        Path project =
            listParent == null
                ? Path.of(entry)
                : listParent.resolve(entry);
        project = project.toAbsolutePath().normalize();
        if (!Files.isDirectory(project)) {
          return Result.err(
              SemanticError.global(
                  ErrorCode.VALIDATION_FAILED, "Not a project directory: " + project));
        }
        Path sourceRoot = project.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
          return Result.err(
              SemanticError.global(
                  ErrorCode.VALIDATION_FAILED, "Missing src/main/java in " + project));
        }
        projects.add(project);
      }
      if (projects.isEmpty()) {
        return Result.err(
            SemanticError.global(ErrorCode.VALIDATION_FAILED, "Projects list is empty"));
      }
      return Result.ok(List.copyOf(projects));
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }
}
