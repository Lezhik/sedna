package io.sedna.dna;

import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticLink;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** SEDNA-FOLD-v1 TLV payload codec for folded motif members. */
final class FoldPayloadCodec {

  record Payload(
      String motifId,
      boolean partialMatch,
      long anchorNodeId,
      List<GenomeNode> members,
      List<SemanticLink> links) {}

  private static final HexFormat HEX = HexFormat.of();

  private FoldPayloadCodec() {}

  static String encodeToConstraint(Payload payload) {
    return SednaFoldV1.FOLD_PAYLOAD_PREFIX + HEX.formatHex(encode(payload));
  }

  static Payload decodeFromConstraint(String constraintCode) {
    if (!constraintCode.startsWith(SednaFoldV1.FOLD_PAYLOAD_PREFIX)) {
      throw new IllegalArgumentException("Not a fold payload constraint: " + constraintCode);
    }
    String hex = constraintCode.substring(SednaFoldV1.FOLD_PAYLOAD_PREFIX.length());
    return decode(HEX.parseHex(hex));
  }

  static byte[] encode(Payload payload) {
    TlvWriter writer = new TlvWriter();
    writer.writeBytes(SednaFoldV1.MAGIC);
    writer.writeU8(SednaFoldV1.VERSION);
    writer.writeUtf8(payload.motifId());
    writer.writeU8(payload.partialMatch() ? 1 : 0);
    writer.writeU64(payload.anchorNodeId());

    TlvWriter membersWriter = new TlvWriter();
    membersWriter.writeU32(payload.members().size());
    for (GenomeNode member : payload.members()) {
      byte[] encoded = DnaCodecSupport.encodeNode(member);
      membersWriter.writeU32(encoded.length);
      membersWriter.writeBytes(encoded);
    }
    writer.writeTlv(SednaFoldV1.TLV_MEMBERS, membersWriter.toByteArray());

    writer.writeTlv(SednaFoldV1.TLV_LINKS, DnaCodecSupport.encodeLinks(payload.links()));
    return writer.toByteArray();
  }

  static Payload decode(byte[] bytes) {
    TlvReader reader = new TlvReader(bytes);
    byte[] magic = reader.readBytes(4);
    if (magic.length != 4
        || magic[0] != SednaFoldV1.MAGIC[0]
        || magic[1] != SednaFoldV1.MAGIC[1]
        || magic[2] != SednaFoldV1.MAGIC[2]
        || magic[3] != SednaFoldV1.MAGIC[3]) {
      throw new IllegalArgumentException("Invalid SEDNA-FOLD-v1 magic");
    }
    int version = reader.readU8();
    if (version != SednaFoldV1.VERSION) {
      throw new IllegalArgumentException("Unsupported SEDNA-FOLD-v1 version: " + version);
    }
    String motifId = reader.readUtf8();
    boolean partialMatch = reader.readU8() == 1;
    long anchorNodeId = reader.readU64();

    List<GenomeNode> members = new ArrayList<>();
    List<SemanticLink> links = List.of();
    while (reader.hasRemaining()) {
      TlvReader.TlvRecord tlv = reader.readTlv();
      if (tlv.type() == SednaFoldV1.TLV_MEMBERS) {
        members = decodeMembers(tlv.payload());
      } else if (tlv.type() == SednaFoldV1.TLV_LINKS) {
        links = DnaCodecSupport.decodeLinks(tlv.payload());
      } else {
        throw new IllegalArgumentException("Unknown fold TLV: " + tlv.type());
      }
    }
    return new Payload(motifId, partialMatch, anchorNodeId, List.copyOf(members), List.copyOf(links));
  }

  private static List<GenomeNode> decodeMembers(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    int count = reader.readU32();
    List<GenomeNode> members = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      byte[] nodeBytes = reader.readBytes(reader.readU32());
      members.add(DnaCodecSupport.decodeNode(nodeBytes));
    }
    return members;
  }
}
