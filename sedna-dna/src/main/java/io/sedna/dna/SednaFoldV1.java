package io.sedna.dna;

/** SEDNA-FOLD-v1 constants (deterministic motif fold/expand payloads). */
public final class SednaFoldV1 {

  static final byte[] MAGIC = new byte[] {'F', 'O', 'L', 'D'};
  public static final int VERSION = 1;

  public static final String MOTIF_CRUD_STACK = "CRUD_STACK";

  public static final String MOTIF_REF_PREFIX = "MOTIF_REF:";
  public static final String FOLD_PAYLOAD_PREFIX = "SEDNA_FOLD_V1:";
  public static final String PARTIAL_MATCH_FLAG = "PARTIAL_MATCH:true";

  public static final int TLV_MEMBERS = 0x01;
  public static final int TLV_LINKS = 0x02;
  public static final int TLV_ANCHOR_NODE_ID = 0x03;

  private SednaFoldV1() {}
}
