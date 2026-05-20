package io.sedna.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SednaCliReverseTest {

  private static final Path CMS_REFERENCE =
      io.sedna.core.examples.ExamplesLayout.findProjectRoot(
              Path.of("..").toAbsolutePath().normalize(), "cms-reference")
          .orElseThrow();

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
