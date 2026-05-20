package io.sedna.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SednaCliPhase14Test {

  @TempDir Path tempDir;

  @Test
  void diffDetectsChangesInJsonMode() throws IOException {
    Path left = tempDir.resolve("left.sdna");
    Path right = tempDir.resolve("right.sdna");
    var graph = CmsReferenceFixtureGraph.create();
    Files.write(left, DnaServices.encoder().encode(graph).value());
    var service =
        graph.nodes().stream().filter(node -> node.kind() == io.sedna.core.NodeKind.SERVICE).findFirst().orElseThrow();
    var updated =
        new io.sedna.core.GenomeNode(
            service.nodeId(),
            service.kind(),
            service.core(),
            service.contracts(),
            java.util.List.of(new io.sedna.core.Constraint("STATELESS_ONLY")));
    var nodes =
        graph.nodes().stream().map(node -> node.nodeId() == service.nodeId() ? updated : node).toList();
    var changed = new io.sedna.core.SemanticGraph(nodes, graph.links(), graph.vocabularyVersion());
    Files.write(right, DnaServices.encoder().encode(changed).value());

    int exit =
        new SednaCli()
            .run(
                new String[] {
                  "diff", "--left=" + left, "--right=" + right, "--format=json"
                });
    assertEquals(1, exit);
  }

  @Test
  void visualizeWritesDotFile() throws IOException {
    Path sdna = tempDir.resolve("fixture.sdna");
    Path dot = tempDir.resolve("graph.dot");
    Files.write(sdna, DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());
    int exit = new SednaCli().run(new String[] {"visualize", "--input=" + sdna, "--output=" + dot});
    assertEquals(0, exit);
    assertTrue(Files.readString(dot).contains("digraph"));
  }

  @Test
  void runAndReplayWithFileCheckpointStore() throws IOException {
    Path sdna = tempDir.resolve("fixture.sdna");
    Path checkpoints = tempDir.resolve("checkpoints");
    Files.write(sdna, DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value());

    int runExit =
        new SednaCli()
            .run(
                new String[] {
                  "run", "--input=" + sdna, "--checkpoint-dir=" + checkpoints, "--format=json"
                });
    assertEquals(0, runExit);

    int replayExit =
        new SednaCli()
            .run(
                new String[] {
                  "replay", "--checkpoint-dir=" + checkpoints, "--format=json"
                });
    assertEquals(0, replayExit);
  }
}
