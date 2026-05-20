package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-015 — runtime replay from file checkpoint store. */
@Tag("e2e")
class RuntimeReplayE2eTest {

  @Test
  void replayProducesIdenticalTraceHash() throws Exception {
    Path base = E2eTestSupport.outputDir("E2E-015");
    E2eTestSupport.prepareDir(base);
    Path checkpoints = base.resolve("checkpoints");
    Path fixture = E2eTestSupport.readGoldenFixture();

    E2eTestSupport.CliResult run =
        E2eTestSupport.runCli(
            "run",
            "--input=" + fixture,
            "--checkpoint-dir=" + checkpoints,
            "--format=json");
    assertEquals(0, run.exitCode(), () -> run.stdout() + run.stderr());
    String traceHash = extractJsonField(run.stdout(), "traceSha256");

    E2eTestSupport.CliResult replay =
        E2eTestSupport.runCli(
            "replay", "--checkpoint-dir=" + checkpoints, "--format=json");
    assertEquals(0, replay.exitCode());
    assertTrue(replay.stdout().contains(traceHash));
  }

  static String extractJsonField(String json, String field) {
    String needle = "\"" + field + "\":\"";
    int start = json.indexOf(needle);
    if (start < 0) {
      throw new IllegalArgumentException("Missing " + field + " in: " + json);
    }
    start += needle.length();
    int end = json.indexOf('"', start);
    return json.substring(start, end);
  }
}
