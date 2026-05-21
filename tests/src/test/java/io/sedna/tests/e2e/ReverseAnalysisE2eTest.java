package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-009 — reverse CMS reference project to DNA. */
@Tag("e2e")
class ReverseAnalysisE2eTest {

  @Test
  void reverseCmsReferenceProducesDna() throws Exception {
    Path project = E2eTestSupport.e2eCmsReferenceProject();
    Path out = E2eTestSupport.outputDir("E2E-009").resolve("reversed.sdna");
    E2eTestSupport.prepareDir(out.getParent());

    E2eTestSupport.CliResult result =
        E2eTestSupport.runCli("reverse", "--input=" + project, "--output=" + out, "--clean");
    assertEquals(0, result.exitCode(), () -> result.stdout() + result.stderr());
    assertTrue(Files.size(out) > 0);
  }
}
