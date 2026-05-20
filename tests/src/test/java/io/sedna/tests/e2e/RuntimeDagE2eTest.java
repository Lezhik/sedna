package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.runtime.RuntimeServices;
import io.sedna.runtime.trace.TraceHasher;
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** E2E-014 — DAG runtime execution via CLI and API. */
@Tag("e2e")
class RuntimeDagE2eTest {

  @Test
  void cliRunMatchesApiTraceHash() throws Exception {
    byte[] dna = Files.readAllBytes(E2eTestSupport.readGoldenFixture());
    var graph = DnaServices.decoder().decode(dna);
    assertTrue(graph.isOk());

    var apiTrace = RuntimeServices.engine().run(graph.value());
    assertTrue(apiTrace.isOk());
    String apiHash = TraceHasher.sha256(apiTrace.value());

    E2eTestSupport.CliResult cli =
        E2eTestSupport.runCli(
            "run", "--input=" + E2eTestSupport.readGoldenFixture(), "--format=json");
    assertEquals(0, cli.exitCode());
    assertTrue(cli.stdout().contains(apiHash));
  }
}
