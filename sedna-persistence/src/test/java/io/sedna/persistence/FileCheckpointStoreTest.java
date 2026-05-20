package io.sedna.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ExecutionToken;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileCheckpointStoreTest {

  @Test
  void appendAndReloadCheckpoints(@TempDir Path temp) {
    FileCheckpointStore store = new FileCheckpointStore(temp);
    byte[] graph = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
    ExecutionToken token = new ExecutionToken(new byte[32]);

    var first = store.append(graph, token, "INIT", 1, "DAG", 0L);
    var second = store.append(graph, token, "INIT", 2, "DAG", 0L);
    assertTrue(first.isOk());
    assertTrue(second.isOk());

    var listed = store.listOrdered();
    assertTrue(listed.isOk());
    assertEquals(2, listed.value().size());
    assertEquals(1L, listed.value().getFirst().sequenceNumber());
    assertEquals(2L, listed.value().getLast().sequenceNumber());
  }
}
