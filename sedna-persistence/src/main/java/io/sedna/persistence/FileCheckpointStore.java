package io.sedna.persistence;

import io.sedna.core.ErrorCode;
import io.sedna.core.ExecutionToken;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Append-only checkpoint directory for local replay (Phase 14). */
public final class FileCheckpointStore implements CheckpointStore {

  private static final String RECORD_PREFIX = "CP|";

  private final Path directory;

  public FileCheckpointStore(Path directory) {
    this.directory = directory.toAbsolutePath().normalize();
  }

  public Path directory() {
    return directory;
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef, ExecutionToken token) {
    return append(graphSnapshotRef, token, "", 0, "DAG", 0L);
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef, ExecutionToken token, String fsmState, int completedNodes) {
    return append(graphSnapshotRef, token, fsmState, completedNodes, "DAG", 0L);
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> append(
      byte[] graphSnapshotRef,
      ExecutionToken token,
      String fsmState,
      int completedNodes,
      String executionProfile,
      long injectFailureNodeId) {
    try {
      Files.createDirectories(directory);
      long sequence = nextSequence();
      long id = sequence;
      CheckpointRecord record =
          new CheckpointRecord(
              id,
              token,
              graphSnapshotRef,
              sequence,
              fsmState,
              completedNodes,
              executionProfile,
              injectFailureNodeId);
      Path file = directory.resolve(String.format(Locale.ROOT, "%06d.cp", sequence));
      Files.writeString(file, encode(record), StandardCharsets.UTF_8);
      return Result.ok(record);
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  @Override
  public synchronized Result<List<CheckpointRecord>, SemanticError> listOrdered() {
    try {
      if (!Files.isDirectory(directory)) {
        return Result.ok(List.of());
      }
      List<CheckpointRecord> records = new ArrayList<>();
      try (var paths = Files.list(directory)) {
        paths
            .filter(
                path -> {
                  var name = path.getFileName();
                  return name != null && name.toString().endsWith(".cp");
                })
            .sorted()
            .forEach(
                path -> {
                  try {
                    records.add(decode(Files.readString(path, StandardCharsets.UTF_8)));
                  } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                  }
                });
      }
      records.sort(Comparator.comparingLong(CheckpointRecord::sequenceNumber));
      return Result.ok(List.copyOf(records));
    } catch (RuntimeException | IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  @Override
  public synchronized Result<CheckpointRecord, SemanticError> findBySequence(long sequenceNumber) {
    var listed = listOrdered();
    if (!listed.isOk()) {
      return Result.err(listed.error());
    }
    for (CheckpointRecord record : listed.value()) {
      if (record.sequenceNumber() == sequenceNumber) {
        return Result.ok(record);
      }
    }
    return Result.err(
        SemanticError.global(
            ErrorCode.VALIDATION_FAILED, "No checkpoint at sequence " + sequenceNumber));
  }

  private long nextSequence() {
    var listed = listOrdered();
    if (!listed.isOk() || listed.value().isEmpty()) {
      return 1L;
    }
    return listed.value().getLast().sequenceNumber() + 1L;
  }

  private static String encode(CheckpointRecord record) {
    return RECORD_PREFIX
        + record.id()
        + '|'
        + record.sequenceNumber()
        + '|'
        + record.executionProfile()
        + '|'
        + record.fsmState()
        + '|'
        + record.completedNodes()
        + '|'
        + record.injectFailureNodeId()
        + '|'
        + HexFormat.of().formatHex(record.executionToken().tokenHash())
        + '|'
        + HexFormat.of().formatHex(record.graphSnapshotRef())
        + '\n';
  }

  private static CheckpointRecord decode(String line) {
    if (!line.startsWith(RECORD_PREFIX)) {
      throw new IllegalArgumentException("Invalid checkpoint record");
    }
    String[] parts = line.trim().split("\\|");
    if (parts.length != 9) {
      throw new IllegalArgumentException("Invalid checkpoint record fields");
    }
    return new CheckpointRecord(
        Long.parseLong(parts[1]),
        new ExecutionToken(HexFormat.of().parseHex(parts[7])),
        HexFormat.of().parseHex(parts[8]),
        Long.parseLong(parts[2]),
        parts[4],
        Integer.parseInt(parts[5]),
        parts[3],
        Long.parseLong(parts[6]));
  }
}
