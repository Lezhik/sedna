package io.sedna.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 checksums for training dataset manifests and reports. */
public final class TrainingManifestHasher {

  private TrainingManifestHasher() {}

  /**
   * Computes the SHA-256 hex digest of UTF-8 text.
   *
   * @param content UTF-8 text to hash
   * @return lowercase SHA-256 hex digest
   */
  public static String sha256(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
