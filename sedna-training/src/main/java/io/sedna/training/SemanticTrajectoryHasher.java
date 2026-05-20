package io.sedna.training;

import io.sedna.training.model.SemanticDelta;
import io.sedna.training.model.SemanticTrajectory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical SHA-256 fingerprint of a semantic trajectory (replay gate). */
public final class SemanticTrajectoryHasher {

  private SemanticTrajectoryHasher() {}

  public static String fingerprint(SemanticTrajectory trajectory) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(trajectory.projectPath().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) '\n');
      for (String commit : trajectory.commitOrder()) {
        digest.update(commit.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ',');
      }
      digest.update((byte) '\n');
      for (var snapshot : trajectory.snapshots()) {
        digest.update(snapshot.commitHash().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ':');
        digest.update(snapshot.dnaFingerprint().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ';');
      }
      digest.update((byte) '\n');
      for (SemanticDelta delta : trajectory.deltas()) {
        digest.update(delta.commitHash().getBytes(StandardCharsets.UTF_8));
        digest.update(longLe(delta.nodeId()));
        digest.update(delta.deltaKind().getBytes(StandardCharsets.UTF_8));
        digest.update(delta.payload().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '|');
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static byte[] longLe(long value) {
    return java.nio.ByteBuffer.allocate(8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putLong(value)
        .array();
  }
}
