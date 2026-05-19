package io.sedna.dna;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.ErrorCode;
import io.sedna.core.GenomeNode;
import io.sedna.core.RegistryVersion;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.core.SemanticLink;
import java.util.ArrayList;
import java.util.List;

/** Default SEDNA-BIN-v1 decoder. */
public final class DefaultDnaDecoder implements DnaDecoder {

  @Override
  public Result<SemanticGraph, SemanticError> decode(byte[] dna) {
    try {
      if (dna == null || dna.length < 6) {
        return Result.err(new SemanticError(ErrorCode.INVALID_DNA, 0L, "DNA too short"));
      }

      TlvReader reader = new TlvReader(dna);
      reader.expectMagic(SednaBinV1.MAGIC);
      int version = reader.readU16();
      if (version != SednaBinV1.VERSION) {
        return Result.err(
            new SemanticError(ErrorCode.INVALID_DNA, 0L, "Unsupported DNA version: " + version));
      }

      byte[] graphPayload = reader.readBytes(reader.hasRemaining() ? dna.length - reader.position() : 0);
      TlvReader graphReader = new TlvReader(graphPayload);

      RegistryVersion registryVersion = null;
      List<GenomeNode> nodes = new ArrayList<>();
      List<SemanticLink> links = List.of();

      while (graphReader.hasRemaining()) {
        TlvReader.TlvRecord tlv = graphReader.readTlv();
        switch (tlv.type()) {
          case SednaBinV1.TLV_GRAPH_HEADER ->
              registryVersion = DnaCodecSupport.decodeRegistryVersion(tlv.payload());
          case SednaBinV1.TLV_NODE_BODY -> nodes.add(DnaCodecSupport.decodeNode(tlv.payload()));
          case SednaBinV1.TLV_LINKS -> links = DnaCodecSupport.decodeLinks(tlv.payload());
          default -> {
            return Result.err(
                new SemanticError(
                    ErrorCode.INVALID_DNA, 0L, "Unknown graph TLV type: " + tlv.type()));
          }
        }
      }

      if (registryVersion == null) {
        return Result.err(new SemanticError(ErrorCode.INVALID_DNA, 0L, "Missing graph header"));
      }

      SemanticGraph graph = new SemanticGraph(nodes, links, registryVersion);
      return Result.ok(CanonicalOrdering.canonicalize(graph));
    } catch (IllegalArgumentException ex) {
      return Result.err(new SemanticError(ErrorCode.INVALID_DNA, 0L, ex.getMessage()));
    }
  }
}
