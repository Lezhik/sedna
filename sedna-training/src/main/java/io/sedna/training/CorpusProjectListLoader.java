package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Loads training projects from {@code examples/cms-list.csv} metadata plus local example folders.
 */
public final class CorpusProjectListLoader {

  private final ProjectListLoader projectListLoader = new ProjectListLoader();

  public Result<List<Path>, SemanticError> loadFromRepository(Path repositoryRoot) {
    Path examples = repositoryRoot.resolve("examples").toAbsolutePath().normalize();
    if (!Files.isDirectory(examples)) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Missing examples directory"));
    }

    Path catalog = examples.resolve("cms-list.csv");
    if (!Files.isRegularFile(catalog)) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Missing cms-list.csv catalog"));
    }

    try {
      TreeSet<String> javaCatalogNames = readJavaCatalogNames(catalog);
      if (javaCatalogNames.isEmpty()) {
        return Result.err(
            SemanticError.global(ErrorCode.VALIDATION_FAILED, "No Java entries in cms-list.csv"));
      }

      TreeSet<Path> localProjects = new TreeSet<>();
      try (var dirs = Files.list(examples)) {
        dirs.filter(Files::isDirectory)
            .filter(dir -> Files.isDirectory(dir.resolve("src/main/java")))
            .map(path -> path.toAbsolutePath().normalize())
            .forEach(localProjects::add);
      }

      if (localProjects.isEmpty()) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED, "No trainable example projects under examples/"));
      }

      Path manifest = examples.resolve("training-corpus.list");
      List<String> manifestLines = new ArrayList<>();
      manifestLines.add("# SEDNA training corpus (deterministic)");
      manifestLines.add("# catalog_java_entries=" + javaCatalogNames.size());
      for (Path project : localProjects) {
        manifestLines.add(project.toString().replace('\\', '/'));
      }
      Files.writeString(manifest, String.join(System.lineSeparator(), manifestLines) + System.lineSeparator());

      return projectListLoader.load(manifest);
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  private static TreeSet<String> readJavaCatalogNames(Path catalog) throws IOException {
    TreeSet<String> names = new TreeSet<>();
    List<String> lines = Files.readAllLines(catalog);
    if (lines.isEmpty()) {
      return names;
    }
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) {
        continue;
      }
      String[] parts = line.split(",", 3);
      if (parts.length < 2) {
        continue;
      }
      String language = parts[1].trim().toLowerCase(Locale.ROOT);
      if (language.equals("java")) {
        names.add(parts[0].trim());
      }
    }
    return names;
  }
}
