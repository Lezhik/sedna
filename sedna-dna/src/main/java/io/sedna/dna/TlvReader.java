package io.sedna.dna;

import io.sedna.core.ErrorCode;
import io.sedna.core.SemanticError;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Little-endian TLV reader for SEDNA-BIN-v1. */
final class TlvReader {

  private final ByteBuffer buffer;

  TlvReader(byte[] data) {
    this.buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
  }

  boolean hasRemaining() {
    return buffer.hasRemaining();
  }

  int position() {
    return buffer.position();
  }

  byte readU8() {
    return buffer.get();
  }

  int readU16() {
    return buffer.getShort() & 0xFFFF;
  }

  int readU32() {
    return buffer.getInt();
  }

  long readU64() {
    return buffer.getLong();
  }

  byte[] readBytes(int length) {
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return bytes;
  }

  String readUtf8() {
    int length = readU32();
    return new String(readBytes(length), StandardCharsets.UTF_8);
  }

  TlvRecord readTlv() {
    if (buffer.remaining() < 6) {
      throw decodeError("Unexpected end of TLV stream");
    }
    int type = readU16();
    int length = readU32();
    if (buffer.remaining() < length) {
      throw decodeError("TLV length exceeds buffer");
    }
    byte[] payload = readBytes(length);
    return new TlvRecord(type, payload);
  }

  void expectMagic(byte[] magic) {
    byte[] actual = readBytes(magic.length);
    for (int i = 0; i < magic.length; i++) {
      if (actual[i] != magic[i]) {
        throw decodeError("Invalid DNA magic");
      }
    }
  }

  private static IllegalArgumentException decodeError(String message) {
    return new IllegalArgumentException(message);
  }

  SemanticError toSemanticError(IllegalArgumentException ex) {
    return new SemanticError(ErrorCode.INVALID_DNA, 0L, ex.getMessage());
  }

  record TlvRecord(int type, byte[] payload) {}
}
