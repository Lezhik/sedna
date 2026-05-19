package io.sedna.dna;

import io.sedna.core.CapabilityRef;
import io.sedna.core.Constraint;
import io.sedna.core.Contract;
import io.sedna.core.ExecutionProfile;
import io.sedna.core.GenomeNode;
import io.sedna.core.LinkType;
import io.sedna.core.NodeKind;
import io.sedna.core.Protocol;
import io.sedna.core.RegistryVersion;
import io.sedna.core.SchemaRef;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticLink;
import io.sedna.core.VocabRef;
import java.util.ArrayList;
import java.util.List;

/** Shared encode/decode helpers for vocab refs, contracts, constraints. */
final class DnaCodecSupport {

  private DnaCodecSupport() {}

  static byte[] encodeVocabRef(VocabRef ref) {
    TlvWriter writer = new TlvWriter();
    writer.writeUtf8(ref.vocabularyId());
    writer.writeUtf8(ref.termPath());
    writer.writeUtf8(ref.version());
    return writer.toByteArray();
  }

  static VocabRef decodeVocabRef(TlvReader reader) {
    return new VocabRef(reader.readUtf8(), reader.readUtf8(), reader.readUtf8());
  }

  static byte[] encodeSemanticCore(SemanticCore core) {
    TlvWriter writer = new TlvWriter();
    writer.writeBytes(encodeVocabRef(core.classRef()));
    writer.writeBytes(encodeVocabRef(core.targetRef()));
    writer.writeBytes(encodeVocabRef(core.operationRef()));
    writer.writeU32(core.modifiers().size());
    for (VocabRef modifier : core.modifiers()) {
      writer.writeBytes(encodeVocabRef(modifier));
    }
    return writer.toByteArray();
  }

  static SemanticCore decodeSemanticCore(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    VocabRef classRef = decodeVocabRef(reader);
    VocabRef targetRef = decodeVocabRef(reader);
    VocabRef operationRef = decodeVocabRef(reader);
    int modifierCount = reader.readU32();
    List<VocabRef> modifiers = new ArrayList<>(modifierCount);
    for (int i = 0; i < modifierCount; i++) {
      modifiers.add(decodeVocabRef(reader));
    }
    return new SemanticCore(classRef, targetRef, operationRef, modifiers);
  }

  static byte[] encodeContracts(List<Contract> contracts) {
    TlvWriter writer = new TlvWriter();
    writer.writeU32(contracts.size());
    for (Contract contract : contracts) {
      writer.writeBytes(encodeContract(contract));
    }
    return writer.toByteArray();
  }

  static byte[] encodeContract(Contract contract) {
    TlvWriter writer = new TlvWriter();
    writer.writeU32(contract.provides().size());
    for (CapabilityRef capability : contract.provides()) {
      writer.writeUtf8(capability.canonical());
    }
    writer.writeU32(contract.requires().size());
    for (CapabilityRef capability : contract.requires()) {
      writer.writeUtf8(capability.canonical());
    }
    writer.writeUtf8(contract.protocol().name());
    writer.writeUtf8(contract.ioSchema().format());
    writer.writeUtf8(contract.ioSchema().payload());
    return writer.toByteArray();
  }

  static List<Contract> decodeContracts(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    int count = reader.readU32();
    List<Contract> contracts = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      contracts.add(decodeContract(reader));
    }
    return contracts;
  }

  static Contract decodeContract(TlvReader reader) {
    int providesCount = reader.readU32();
    List<CapabilityRef> provides = new ArrayList<>(providesCount);
    for (int i = 0; i < providesCount; i++) {
      provides.add(parseCapability(reader.readUtf8()));
    }
    int requiresCount = reader.readU32();
    List<CapabilityRef> requires = new ArrayList<>(requiresCount);
    for (int i = 0; i < requiresCount; i++) {
      requires.add(parseCapability(reader.readUtf8()));
    }
    Protocol protocol = Protocol.valueOf(reader.readUtf8());
    SchemaRef schema = new SchemaRef(reader.readUtf8(), reader.readUtf8());
    return new Contract(provides, requires, protocol, schema);
  }

  static byte[] encodeConstraints(List<Constraint> constraints) {
    TlvWriter writer = new TlvWriter();
    writer.writeU32(constraints.size());
    for (Constraint constraint : constraints) {
      writer.writeUtf8(constraint.code());
    }
    return writer.toByteArray();
  }

  static List<Constraint> decodeConstraints(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    int count = reader.readU32();
    List<Constraint> constraints = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      constraints.add(new Constraint(reader.readUtf8()));
    }
    return constraints;
  }

  static byte[] encodeNode(GenomeNode node) {
    TlvWriter writer = new TlvWriter();
    writer.writeU64(node.nodeId());
    writer.writeU16(node.kind().code());
    writer.writeU8(ExecutionProfile.DAG.code());
    writer.writeTlv(SednaBinV1.TLV_SEMANTIC_CORE, encodeSemanticCore(node.core()));
    writer.writeTlv(SednaBinV1.TLV_CONTRACTS, encodeContracts(node.contracts()));
    writer.writeTlv(SednaBinV1.TLV_CONSTRAINTS, encodeConstraints(node.constraints()));
    return writer.toByteArray();
  }

  static GenomeNode decodeNode(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    long nodeId = reader.readU64();
    NodeKind kind = NodeKind.fromCode(reader.readU16());
    ExecutionProfile.fromCode(reader.readU8());
    SemanticCore core = null;
    List<Contract> contracts = List.of();
    List<Constraint> constraints = List.of();
    while (reader.hasRemaining()) {
      TlvReader.TlvRecord tlv = reader.readTlv();
      switch (tlv.type()) {
        case SednaBinV1.TLV_SEMANTIC_CORE -> core = decodeSemanticCore(tlv.payload());
        case SednaBinV1.TLV_CONTRACTS -> contracts = decodeContracts(tlv.payload());
        case SednaBinV1.TLV_CONSTRAINTS -> constraints = decodeConstraints(tlv.payload());
        default -> throw new IllegalArgumentException("Unknown node TLV type: " + tlv.type());
      }
    }
    if (core == null) {
      throw new IllegalArgumentException("Missing semantic core");
    }
    return new GenomeNode(nodeId, kind, core, contracts, constraints);
  }

  static byte[] encodeLinks(List<SemanticLink> links) {
    TlvWriter writer = new TlvWriter();
    writer.writeU32(links.size());
    for (SemanticLink link : links) {
      writer.writeU64(link.sourceNodeId());
      writer.writeU64(link.targetNodeId());
      writer.writeU16(link.type().ordinal());
    }
    return writer.toByteArray();
  }

  static List<SemanticLink> decodeLinks(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    int count = reader.readU32();
    List<SemanticLink> links = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      long source = reader.readU64();
      long target = reader.readU64();
      LinkType type = LinkType.values()[reader.readU16()];
      links.add(new SemanticLink(source, target, type));
    }
    return links;
  }

  static byte[] encodeRegistryVersion(RegistryVersion version) {
    TlvWriter writer = new TlvWriter();
    writer.writeUtf8(version.vocabularyId());
    writer.writeU16(version.major());
    writer.writeU16(version.minor());
    return writer.toByteArray();
  }

  static RegistryVersion decodeRegistryVersion(byte[] payload) {
    TlvReader reader = new TlvReader(payload);
    return new RegistryVersion(reader.readUtf8(), reader.readU16(), reader.readU16());
  }

  static CapabilityRef parseCapability(String canonical) {
    int at = canonical.lastIndexOf('@');
    if (at < 1) {
      throw new IllegalArgumentException("Invalid capability: " + canonical);
    }
    return new CapabilityRef(canonical.substring(0, at), canonical.substring(at + 1));
  }
}
