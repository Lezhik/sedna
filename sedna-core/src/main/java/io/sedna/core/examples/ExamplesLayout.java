package io.sedna.core.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Canonical paths under the repository {@code examples/} tree. */
public final class ExamplesLayout {

  public static final String ROOT = "examples";
  public static final String DOCS = "examples/docs";
  public static final String CMS_LIST_CATALOG = "examples/docs/cms-list.csv";
  public static final String TRAINING_CORPUS_MANIFEST = "examples/docs/training-corpus.list";
  public static final String TRAINING_PROJECTS_MANIFEST = "examples/docs/training-projects.txt";
  public static final String E2E_TESTS = "examples/sedna-e2e-tests";
  public static final String E2E_CMS_REFERENCE_PROJECT = E2E_TESTS + "/cms-reference";
  public static final String E2E_TRAINING_PROJECTS_MANIFEST = E2E_TESTS + "/e2e-training-projects.txt";
  public static final String GOLDEN_CMS_FIXTURE = E2E_TESTS + "/cms-reference-fixture.sdna";
  public static final String GOLDEN_CMS_FIXTURE_DOC = "examples/docs/cms-reference-fixture.md";

  private static final String CATEGORY_PREFIX = "sedna-";

  private ExamplesLayout() {}

  public static Path examplesRoot(Path repositoryRoot) {
    return repositoryRoot.resolve(ROOT).toAbsolutePath().normalize();
  }

  public static Path goldenCmsFixture(Path repositoryRoot) {
    return repositoryRoot.resolve(GOLDEN_CMS_FIXTURE).toAbsolutePath().normalize();
  }

  public static Path goldenCmsFixtureDoc(Path repositoryRoot) {
    return repositoryRoot.resolve(GOLDEN_CMS_FIXTURE_DOC).toAbsolutePath().normalize();
  }

  /** Spring Boot CMS reference used by E2E reverse/forward scenarios. */
  public static Path e2eCmsReferenceProject(Path repositoryRoot) {
    return repositoryRoot.resolve(E2E_CMS_REFERENCE_PROJECT).toAbsolutePath().normalize();
  }

  public static Path e2eTrainingProjectsManifest(Path repositoryRoot) {
    return repositoryRoot.resolve(E2E_TRAINING_PROJECTS_MANIFEST).toAbsolutePath().normalize();
  }

  /**
   * Resolves a trainable Gradle project by folder name (e.g. {@code cms-reference},
   * {@code spring-demo}) under {@code examples/sedna-* /}.
   */
  public static Optional<Path> findProjectRoot(Path repositoryRoot, String projectFolderName) {
    Path examples = examplesRoot(repositoryRoot);
    if (!Files.isDirectory(examples)) {
      return Optional.empty();
    }
    Path direct = examples.resolve(projectFolderName);
    if (Files.isDirectory(direct.resolve("src/main/java"))) {
      return Optional.of(direct.toAbsolutePath().normalize());
    }
    try (var categories = Files.list(examples)) {
      return categories
          .filter(Files::isDirectory)
          .filter(ExamplesLayout::isSednaCategory)
          .map(category -> category.resolve(projectFolderName))
          .filter(candidate -> Files.isDirectory(candidate.resolve("src/main/java")))
          .map(path -> path.toAbsolutePath().normalize())
          .findFirst();
    } catch (IOException ex) {
      return Optional.empty();
    }
  }

  /** Lists all local trainable projects under {@code examples/sedna-* /}. */
  public static List<Path> listTrainableProjects(Path repositoryRoot) throws IOException {
    Path examples = examplesRoot(repositoryRoot);
    List<Path> projects = new ArrayList<>();
    if (!Files.isDirectory(examples)) {
      return projects;
    }
    try (var categories = Files.list(examples)) {
      categories
          .filter(Files::isDirectory)
          .filter(ExamplesLayout::isSednaCategory)
          .sorted(Comparator.comparing(ExamplesLayout::categoryName))
          .forEach(
              category -> {
                try (var children = Files.list(category)) {
                  children
                      .filter(Files::isDirectory)
                      .filter(dir -> Files.isDirectory(dir.resolve("src/main/java")))
                      .map(path -> path.toAbsolutePath().normalize())
                      .sorted(Comparator.comparing(ExamplesLayout::categoryName))
                      .forEach(projects::add);
                } catch (IOException ignored) {
                  // skip unreadable category
                }
              });
    }
    return projects;
  }

  private static boolean isSednaCategory(Path dir) {
    return categoryName(dir).startsWith(CATEGORY_PREFIX);
  }

  private static String categoryName(Path path) {
    var name = path.getFileName();
    return name == null ? "" : name.toString();
  }
}
