package io.sedna.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ExecutionToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class JdbcCheckpointStoreTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Test
  void appendAndListOrdered() throws java.sql.SQLException, java.security.NoSuchAlgorithmException {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(POSTGRES.getJdbcUrl());
    dataSource.setUser(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());

    JdbcCheckpointStore store = new JdbcCheckpointStore(dataSource);
    store.migrate();

    byte[] snapshot = "snapshot-v1".getBytes(StandardCharsets.UTF_8);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    ExecutionToken token1 = new ExecutionToken(digest.digest("token-1".getBytes(StandardCharsets.UTF_8)));
    ExecutionToken token2 = new ExecutionToken(digest.digest("token-2".getBytes(StandardCharsets.UTF_8)));

    assertTrue(store.append(snapshot, token1).isOk());
    assertTrue(store.append(snapshot, token2).isOk());

    var listed = store.listOrdered();
    assertTrue(listed.isOk());
    assertEquals(2, listed.value().size());
    assertEquals(1L, listed.value().get(0).sequenceNumber());
    assertEquals(2L, listed.value().get(1).sequenceNumber());
  }
}
