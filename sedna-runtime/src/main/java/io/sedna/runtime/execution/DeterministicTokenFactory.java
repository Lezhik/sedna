package io.sedna.runtime.execution;

import io.sedna.core.ExecutionToken;
import io.sedna.core.GenomeNode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Deterministic SHA-256 execution tokens (no randomness). */
public final class DeterministicTokenFactory {

  private DeterministicTokenFactory() {}

  public static ExecutionToken token(GenomeNode node, long sequenceNumber) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(longLe(node.nodeId()));
      digest.update(longLe(sequenceNumber));
      digest.update(utf8(node.kind().name()));
      digest.update(utf8(node.core().classRef().canonicalKey()));
      digest.update(utf8(node.core().targetRef().canonicalKey()));
      digest.update(utf8(node.core().operationRef().canonicalKey()));
      return new ExecutionToken(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static byte[] longLe(long value) {
    return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
  }

  private static byte[] utf8(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    byte[] length = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.length).array();
    byte[] combined = new byte[length.length + bytes.length];
    System.arraycopy(length, 0, combined, 0, length.length);
    System.arraycopy(bytes, 0, combined, length.length, bytes.length);
    return combined;
  }
}
