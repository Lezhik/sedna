package io.sedna.registry;

/** TLV constants for registry extension payloads (REG-EXT-v1). */
final class RegistryExtensionTlv {

  static final byte[] MAGIC = {'R', 'E', 'G', '1'};
  static final int VERSION = 1;
  static final int TLV_VOCAB_ENTRY = 0x01;

  private RegistryExtensionTlv() {}
}
