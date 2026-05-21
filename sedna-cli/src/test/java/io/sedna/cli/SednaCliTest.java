package io.sedna.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SednaCliTest {

  @TempDir Path tempDir;

  @Test
  void validateFixtureDna() throws java.io.IOException {
    Path sdna = tempDir.resolve("fixture.sdna");
    Files.write(sdna, DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());
    int exit = new SednaCli().run(new String[] {"validate", "--input=" + sdna});
    assertEquals(0, exit);
  }

  @Test
  void helpExitsZero() {
    assertEquals(0, new SednaCli().run(new String[] {"--help"}));
    assertEquals(0, new SednaCli().run(new String[] {"help"}));
  }

  @Test
  void forwardWritesOutput() throws java.io.IOException {
    Path sdna = tempDir.resolve("fixture.sdna");
    Path output = tempDir.resolve("generated");
    Files.write(sdna, DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());
    int exit =
        new SednaCli().run(new String[] {"forward", "--input=" + sdna, "--output=" + output});
    assertEquals(0, exit);
    assertTrue(Files.exists(output.resolve("build.gradle.kts")));
  }

  @Test
  void forwardCleanRemovesStaleArtifacts() throws java.io.IOException {
    Path sdna = tempDir.resolve("fixture.sdna");
    Path output = tempDir.resolve("generated");
    Files.write(sdna, DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());
    Files.createDirectories(output);
    Path stale = output.resolve("stale-marker.txt");
    Files.writeString(stale, "old");

    int exit =
        new SednaCli()
            .run(
                new String[] {
                  "forward", "--input=" + sdna, "--output=" + output, "--clean"
                });
    assertEquals(0, exit);
    assertTrue(Files.exists(output.resolve("build.gradle.kts")));
    assertTrue(!Files.exists(stale));
  }
}
