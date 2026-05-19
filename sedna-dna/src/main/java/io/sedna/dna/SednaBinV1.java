package io.sedna.dna;

/** SEDNA-BIN-v1 constants. */
final class SednaBinV1 {

  static final byte[] MAGIC = {'S', 'D', 'N', '1'};
  static final int VERSION = 1;

  static final int TLV_SEMANTIC_CORE = 0x01;
  static final int TLV_CONTRACTS = 0x02;
  static final int TLV_CONSTRAINTS = 0x03;
  static final int TLV_NODE_BODY = 0x10;
  static final int TLV_LINKS = 0x20;
  static final int TLV_GRAPH_HEADER = 0x30;

  private SednaBinV1() {}
}
