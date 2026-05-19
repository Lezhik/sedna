package io.sedna.dna;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Little-endian TLV writer for SEDNA-BIN-v1. */
final class TlvWriter {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  void writeBytes(byte[] value) {
    out.writeBytes(value);
  }

  void writeU8(int value) {
    out.write(value & 0xFF);
  }

  void writeU16(int value) {
    writeBytes(shortLe((short) value));
  }

  void writeU32(int value) {
    writeBytes(intLe(value));
  }

  void writeU64(long value) {
    writeBytes(longLe(value));
  }

  void writeUtf8(String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    writeU32(bytes.length);
    writeBytes(bytes);
  }

  void writeTlv(int type, byte[] payload) {
    writeU16(type);
    writeU32(payload.length);
    writeBytes(payload);
  }

  byte[] toByteArray() {
    return out.toByteArray();
  }

  private static byte[] shortLe(short value) {
    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
  }

  private static byte[] intLe(int value) {
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
  }

  private static byte[] longLe(long value) {
    return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
  }
}
