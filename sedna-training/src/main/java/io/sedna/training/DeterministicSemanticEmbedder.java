package io.sedna.training;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticGraph;
import io.sedna.training.model.SemanticEmbedding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** SEDNA-EMBED-v1: deterministic SHA-256 over vocabulary path + contract signature. */
public final class DeterministicSemanticEmbedder {

  /** Creates an embedder using SEDNA-EMBED-v1 rules. */
  public DeterministicSemanticEmbedder() {}

  /**
   * Builds deterministic embeddings for all nodes in a graph.
   *
   * @param graph semantic graph to embed
   * @return embeddings sorted by {@code nodeId}
   */
  public List<SemanticEmbedding> embed(SemanticGraph graph) {
    SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
    List<SemanticEmbedding> embeddings = new ArrayList<>();
    for (GenomeNode node : canonical.nodes()) {
      String vocabularyPath = vocabularyPath(node.core());
      String signature = contractSignature(node);
      String hex = sha256(vocabularyPath + "|" + signature);
      embeddings.add(new SemanticEmbedding(node.nodeId(), vocabularyPath, hex));
    }
    embeddings.sort(Comparator.comparingLong(SemanticEmbedding::nodeId));
    return List.copyOf(embeddings);
  }

  private static String vocabularyPath(SemanticCore core) {
    return core.classRef().canonicalKey()
        + ">"
        + core.targetRef().canonicalKey()
        + ">"
        + core.operationRef().canonicalKey();
  }

  private static String contractSignature(GenomeNode node) {
    StringBuilder builder = new StringBuilder();
    for (Contract contract : node.contracts()) {
      builder.append(contract.protocol().name()).append('|');
      contract.provides().forEach(cap -> appendCapability(builder, cap.name(), cap.versionConstraint()));
      contract.requires().forEach(cap -> appendCapability(builder, cap.name(), cap.versionConstraint()));
    }
    return builder.toString();
  }

  private static void appendCapability(StringBuilder builder, String name, String version) {
    builder.append(name).append('@').append(version).append(',');
  }

  private static String sha256(String payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
