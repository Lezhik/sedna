package io.sedna.persistence;

import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** PostgreSQL-compatible JDBC checkpoint store. */
public final class JdbcCheckpointStore implements CheckpointStore {

  private final DataSource dataSource;

  public JdbcCheckpointStore(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void migrate() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE TABLE IF NOT EXISTS sedna_checkpoint (
            id BIGSERIAL PRIMARY KEY,
            execution_token BYTEA NOT NULL UNIQUE,
            graph_snapshot_ref BYTEA NOT NULL,
            sequence_number BIGINT NOT NULL UNIQUE
          )
          """);
    }
  }

  @Override
  public Result<CheckpointRecord, SemanticError> append(byte[] graphSnapshotRef, ExecutionToken token) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                INSERT INTO sedna_checkpoint (execution_token, graph_snapshot_ref, sequence_number)
                VALUES (?, ?, (SELECT COALESCE(MAX(sequence_number), 0) + 1 FROM sedna_checkpoint))
                RETURNING id, sequence_number
                """,
                Statement.RETURN_GENERATED_KEYS)) {
      statement.setBytes(1, token.tokenHash());
      statement.setBytes(2, graphSnapshotRef);
      try (ResultSet keys = statement.executeQuery()) {
        if (!keys.next()) {
          return Result.err(
              SemanticError.global(ErrorCode.INTERNAL, "Checkpoint insert returned no keys"));
        }
        long id = keys.getLong("id");
        long sequence = keys.getLong("sequence_number");
        return Result.ok(new CheckpointRecord(id, token, graphSnapshotRef, sequence));
      }
    } catch (SQLException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  @Override
  public Result<List<CheckpointRecord>, SemanticError> listOrdered() {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT id, execution_token, graph_snapshot_ref, sequence_number
                FROM sedna_checkpoint
                ORDER BY sequence_number ASC
                """)) {
      List<CheckpointRecord> records = new ArrayList<>();
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          records.add(readRow(rs));
        }
      }
      return Result.ok(records);
    } catch (SQLException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  @Override
  public Result<CheckpointRecord, SemanticError> findBySequence(long sequenceNumber) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT id, execution_token, graph_snapshot_ref, sequence_number
                FROM sedna_checkpoint
                WHERE sequence_number = ?
                """)) {
      statement.setLong(1, sequenceNumber);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return Result.err(
              SemanticError.global(
                  ErrorCode.VALIDATION_FAILED, "No checkpoint at sequence " + sequenceNumber));
        }
        return Result.ok(readRow(rs));
      }
    } catch (SQLException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  private static CheckpointRecord readRow(ResultSet rs) throws SQLException {
    return new CheckpointRecord(
        rs.getLong("id"),
        new ExecutionToken(rs.getBytes("execution_token")),
        rs.getBytes("graph_snapshot_ref"),
        rs.getLong("sequence_number"));
  }
}
