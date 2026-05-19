package io.sedna.dna;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;

/** Default SEDNA-BIN-v1 encoder with canonical ordering (FR-dna.02). */
public final class DefaultDnaEncoder implements DnaEncoder {

  @Override
  public Result<byte[], SemanticError> encode(SemanticGraph graph) {
    try {
      SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
      TlvWriter writer = new TlvWriter();
      writer.writeBytes(SednaBinV1.MAGIC);
      writer.writeU16(SednaBinV1.VERSION);

      TlvWriter graphWriter = new TlvWriter();
      graphWriter.writeTlv(
          SednaBinV1.TLV_GRAPH_HEADER,
          DnaCodecSupport.encodeRegistryVersion(canonical.vocabularyVersion()));
      for (var node : canonical.nodes()) {
        graphWriter.writeTlv(SednaBinV1.TLV_NODE_BODY, DnaCodecSupport.encodeNode(node));
      }
      graphWriter.writeTlv(SednaBinV1.TLV_LINKS, DnaCodecSupport.encodeLinks(canonical.links()));
      writer.writeBytes(graphWriter.toByteArray());

      return Result.ok(writer.toByteArray());
    } catch (RuntimeException ex) {
      return Result.err(new SemanticError(ErrorCode.INVALID_DNA, 0L, ex.getMessage()));
    }
  }
}
