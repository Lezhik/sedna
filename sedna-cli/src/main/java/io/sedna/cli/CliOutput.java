package io.sedna.cli;

import io.sedna.core.SemanticError;
import io.sedna.validation.SemanticGraphDiffEntry;
import java.util.List;

/** Structured CLI output (Phase 14 {@code --format=json}). */
public final class CliOutput {

  /** CLI output format. */
  public enum Format {
    /** Human-readable text on stdout/stderr. */
    TEXT,
    /** Single-line JSON objects for CI automation. */
    JSON;

    /**
     * Parses a format flag value.
     *
     * @param value raw {@code --format=} value (null or blank defaults to TEXT)
     * @return parsed format
     */
    public static Format parse(String value) {
      if (value == null || value.isBlank()) {
        return TEXT;
      }
      return "json".equalsIgnoreCase(value.trim()) ? JSON : TEXT;
    }
  }

  private CliOutput() {}

  /**
   * Prints a successful command result.
   *
   * @param format output format
   * @param command command name
   * @param message human-readable message
   * @param extraJsonFields optional extra JSON fields (without leading comma)
   */
  public static void printSuccess(Format format, String command, String message, String extraJsonFields) {
    if (format == Format.JSON) {
      System.out.println(
          "{\"status\":\"ok\",\"command\":\""
              + escape(command)
              + "\",\"message\":\""
              + escape(message)
              + "\""
              + (extraJsonFields == null || extraJsonFields.isBlank() ? "" : "," + extraJsonFields)
              + "}");
      return;
    }
    System.out.println(message);
  }

  /**
   * Prints a semantic error.
   *
   * @param format output format
   * @param error structured error
   */
  public static void printError(Format format, SemanticError error) {
    if (format == Format.JSON) {
      System.out.println(
          "{\"status\":\"error\",\"code\":\""
              + escape(error.code().name())
              + "\",\"nodeId\":"
              + error.nodeId()
              + ",\"message\":\""
              + escape(error.message())
              + "\"}");
      return;
    }
    System.err.println(error.code() + " [nodeId=" + error.nodeId() + "]: " + error.message());
  }

  /**
   * Prints semantic graph diff results.
   *
   * @param format output format
   * @param diffs diff entries (empty when equivalent)
   * @param equivalent whether graphs are semantically equivalent
   */
  public static void printDiff(Format format, List<SemanticGraphDiffEntry> diffs, boolean equivalent) {
    if (format == Format.JSON) {
      StringBuilder builder = new StringBuilder();
      builder.append("{\"status\":\"ok\",\"command\":\"diff\",\"equivalent\":").append(equivalent).append(",\"deltas\":[");
      for (int i = 0; i < diffs.size(); i++) {
        SemanticGraphDiffEntry entry = diffs.get(i);
        if (i > 0) {
          builder.append(',');
        }
        builder
            .append("{\"nodeId\":")
            .append(entry.nodeId())
            .append(",\"kind\":\"")
            .append(escape(entry.kind()))
            .append("\",\"payload\":\"")
            .append(escape(entry.payload()))
            .append("\"}");
      }
      builder.append("]}");
      System.out.println(builder);
      return;
    }
    System.out.println("equivalent=" + equivalent + " deltas=" + diffs.size());
    for (SemanticGraphDiffEntry entry : diffs) {
      System.out.println(entry.kind() + " nodeId=" + entry.nodeId() + " " + entry.payload());
    }
  }

  /**
   * Prints replay completion summary.
   *
   * @param format output format
   * @param events number of replayed events
   * @param traceSha256 SHA-256 of replay trace
   * @param checkpointSequence last checkpoint sequence number
   */
  public static void printReplay(Format format, int events, String traceSha256, long checkpointSequence) {
    if (format == Format.JSON) {
      System.out.println(
          "{\"status\":\"ok\",\"command\":\"replay\",\"checkpointSequence\":"
              + checkpointSequence
              + ",\"events\":"
              + events
              + ",\"traceSha256\":\""
              + escape(traceSha256)
              + "\"}");
      return;
    }
    System.out.println(
        "Replay completed: checkpointSequence="
            + checkpointSequence
            + " events="
            + events
            + " traceSha256="
            + traceSha256);
  }

  private static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
