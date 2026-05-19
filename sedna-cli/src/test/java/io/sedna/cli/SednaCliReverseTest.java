package io.sedna.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SednaCliReverseTest {

  private static final Path CMS_REFERENCE =
      Paths.get("..", "examples", "cms-reference").normalize().toAbsolutePath();

  @Test
  void reverseCommandWritesDna(@TempDir Path tempDir) throws java.io.IOException {
    Path output = tempDir.resolve("out.sdna");
    int exit =
        new SednaCli()
            .run(new String[] {"reverse", "--input=" + CMS_REFERENCE, "--output=" + output});
    assertEquals(0, exit);
    assertTrue(Files.size(output) > 0);
  }
}
