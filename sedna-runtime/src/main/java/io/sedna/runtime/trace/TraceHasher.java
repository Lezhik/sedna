package io.sedna.runtime.trace;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical trace hash for replay comparison (excludes non-semantic fields). */
public final class TraceHasher {

  private TraceHasher() {}

  public static String sha256(ExecutionTrace trace) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (ExecutionTraceEvent event : trace.events()) {
        digest.update(longLe(event.sequenceNumber()));
        digest.update(longLe(event.nodeId()));
        digest.update(utf8(event.kind().name()));
        digest.update(event.token().tokenHash());
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static byte[] utf8(String value) {
    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static byte[] longLe(long value) {
    return java.nio.ByteBuffer.allocate(8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putLong(value)
        .array();
  }
}
