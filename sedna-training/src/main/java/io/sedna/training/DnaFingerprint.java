package io.sedna.training;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Canonical SHA-256 fingerprint of encoded DNA bytes. */
public final class DnaFingerprint {

  private DnaFingerprint() {}

  /**
   * Computes the SHA-256 fingerprint of encoded DNA for a graph.
   *
   * @param graph semantic graph to encode
   * @param encoder DNA encoder
   * @return hex fingerprint or structured error
   */
  public static Result<String, SemanticError> of(SemanticGraph graph, DnaEncoder encoder) {
    var encoded = encoder.encode(graph);
    if (!encoded.isOk()) {
      return Result.err(encoded.error());
    }
    return Result.ok(sha256(encoded.value()));
  }

  /**
   * Computes SHA-256 hex digest of raw bytes.
   *
   * @param bytes input bytes
   * @return lowercase hex digest
   */
  public static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(bytes);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
