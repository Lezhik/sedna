package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-006 — CLI forward generates Gradle project tree. */
@Tag("e2e")
class ForwardGenerateE2eTest {

  @Test
  void forwardWritesGradleProject() throws Exception {
    Path out = E2eTestSupport.outputDir("E2E-006").resolve("generated");
    E2eTestSupport.prepareDir(out.getParent());
    E2eTestSupport.prepareDir(out);

    Path fixture = E2eTestSupport.readGoldenFixture();
    E2eTestSupport.CliResult result =
        E2eTestSupport.runCli("forward", "--input=" + fixture, "--output=" + out);
    assertEquals(0, result.exitCode(), () -> result.stdout() + result.stderr());
    assertTrue(Files.isRegularFile(out.resolve("build.gradle.kts")));
  }
}
