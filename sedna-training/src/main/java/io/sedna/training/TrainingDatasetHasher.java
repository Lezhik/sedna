package io.sedna.training;

import io.sedna.training.model.TrainingDataset;
import io.sedna.training.model.TrainingProjectResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical fingerprint of a multi-project training dataset. */
public final class TrainingDatasetHasher {

  private TrainingDatasetHasher() {}

  public static String fingerprint(TrainingDataset dataset) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (TrainingProjectResult project : dataset.projects()) {
        digest.update(project.projectPath().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
        for (String commit : project.trajectory().commitOrder()) {
          digest.update(commit.getBytes(StandardCharsets.UTF_8));
          digest.update((byte) ',');
        }
        digest.update((byte) '\n');
        for (var snapshot : project.trajectory().snapshots()) {
          digest.update(snapshot.dnaFingerprint().getBytes(StandardCharsets.UTF_8));
          digest.update((byte) ';');
        }
        digest.update((byte) '\n');
        for (var embedding : project.embeddings()) {
          digest.update(longLe(embedding.nodeId()));
          digest.update(embedding.embeddingHex().getBytes(StandardCharsets.UTF_8));
        }
        digest.update((byte) '\n');
        for (var mutation : project.mutationDataset()) {
          digest.update(longLe(mutation.targetNodeId()));
          digest.update(mutation.operation().name().getBytes(StandardCharsets.UTF_8));
          digest.update(mutation.label().getBytes(StandardCharsets.UTF_8));
        }
        digest.update((byte) '\n');
        for (var proposal : project.registryProposals()) {
          digest.update(proposal.proposed().canonicalKey().getBytes(StandardCharsets.UTF_8));
          digest.update(proposal.resolution().getBytes(StandardCharsets.UTF_8));
        }
        digest.update((byte) '\n');
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
