package io.sedna.dna;

import io.sedna.core.GenomeNode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 based stable NodeID (first 64 bits). */
final class NodeIdHasher {

  private NodeIdHasher() {}

  static long hash(GenomeNode node) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      updateUtf8(digest, node.kind().name());
      updateUtf8(digest, node.core().classRef().canonicalKey());
      updateUtf8(digest, node.core().targetRef().canonicalKey());
      updateUtf8(digest, node.core().operationRef().canonicalKey());
      for (var modifier : node.core().modifiers()) {
        updateUtf8(digest, modifier.canonicalKey());
      }
      byte[] hash = digest.digest();
      ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
      return buffer.getLong();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static void updateUtf8(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update(intLe(bytes.length));
    digest.update(bytes);
  }

  private static byte[] intLe(int value) {
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
  }
}
