package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class CorpusProjectListLoaderTest {

  private static final Path REPO_ROOT =
      Paths.get("..").toAbsolutePath().normalize();

  @Test
  void loadsLocalExampleProjectsFromCatalog() {
    var loaded = new CorpusProjectListLoader().loadFromRepository(REPO_ROOT);
    assertTrue(loaded.isOk(), () -> String.valueOf(loaded.error()));
    assertFalse(loaded.value().isEmpty());
    assertTrue(
        loaded.value().stream()
            .anyMatch(path -> path.toString().replace('\\', '/').endsWith("spring-demo")));
  }
}
