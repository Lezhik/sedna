package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.examples.ExamplesLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Loads training projects from {@code examples/docs/cms-list.csv} metadata plus local projects
 * under {@code examples/sedna-* /}.
 */
public final class CorpusProjectListLoader {

  private final ProjectListLoader projectListLoader = new ProjectListLoader();

  /** Creates a corpus loader with default project list parsing. */
  public CorpusProjectListLoader() {}

  /**
   * Discovers local trainable projects and writes {@code training-corpus.list}.
   *
   * @param repositoryRoot SEDNA repository root
   * @return ordered project paths or structured error
   */
  public Result<List<Path>, SemanticError> loadFromRepository(Path repositoryRoot) {
    Path examples = ExamplesLayout.examplesRoot(repositoryRoot);
    if (!Files.isDirectory(examples)) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Missing examples directory"));
    }

    Path catalog = repositoryRoot.resolve(ExamplesLayout.CMS_LIST_CATALOG).toAbsolutePath().normalize();
    if (!Files.isRegularFile(catalog)) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "Missing catalog: " + ExamplesLayout.CMS_LIST_CATALOG));
    }

    try {
      TreeSet<String> javaCatalogNames = readJavaCatalogNames(catalog);
      if (javaCatalogNames.isEmpty()) {
        return Result.err(
            SemanticError.global(ErrorCode.VALIDATION_FAILED, "No Java entries in cms-list.csv"));
      }

      List<Path> localProjects = ExamplesLayout.listTrainableProjects(repositoryRoot);
      if (localProjects.isEmpty()) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "No trainable example projects under examples/sedna-*"));
      }

      Path manifest =
          repositoryRoot.resolve(ExamplesLayout.TRAINING_CORPUS_MANIFEST).toAbsolutePath().normalize();
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
