package io.sedna.registry;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticDefinition;
import io.sedna.core.SemanticError;
import io.sedna.core.VocabRef;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Decodes REG-EXT-v1 registry extension TLV payloads. */
public final class TlvRegistryExtensionDecoder implements RegistryExtensionDecoder {

  @Override
  public Result<Map<String, SemanticDefinition>, SemanticError> decode(byte[] extensionPayload) {
    if (extensionPayload == null || extensionPayload.length == 0) {
      return Result.ok(Map.of());
    }
    try {
      ByteBuffer buffer = ByteBuffer.wrap(extensionPayload).order(ByteOrder.LITTLE_ENDIAN);
      byte[] magic = readBytes(buffer, RegistryExtensionTlv.MAGIC.length);
      for (int i = 0; i < RegistryExtensionTlv.MAGIC.length; i++) {
        if (magic[i] != RegistryExtensionTlv.MAGIC[i]) {
          return err(ErrorCode.INVALID_DNA, "Invalid registry extension magic");
        }
      }
      int version = buffer.get() & 0xFF;
      if (version != RegistryExtensionTlv.VERSION) {
        return err(ErrorCode.INVALID_DNA, "Unsupported registry extension version: " + version);
      }
      Map<String, SemanticDefinition> definitions = new LinkedHashMap<>();
      while (buffer.hasRemaining()) {
        if (buffer.remaining() < 6) {
          return err(ErrorCode.INVALID_DNA, "Truncated registry extension TLV");
        }
        int type = buffer.getShort() & 0xFFFF;
        int length = buffer.getInt();
        if (buffer.remaining() < length) {
          return err(ErrorCode.INVALID_DNA, "Registry extension TLV length exceeds buffer");
        }
        byte[] payload = readBytes(buffer, length);
        if (type != RegistryExtensionTlv.TLV_VOCAB_ENTRY) {
          return err(ErrorCode.INVALID_DNA, "Unknown registry extension TLV type: " + type);
        }
        var entry = decodeVocabEntry(payload);
        if (!entry.isOk()) {
          return Result.err(entry.error());
        }
        SemanticDefinition definition = entry.value();
        definitions.put(definition.ref().canonicalKey(), definition);
      }
      return Result.ok(Map.copyOf(definitions));
    } catch (IllegalArgumentException ex) {
      return err(ErrorCode.INVALID_DNA, ex.getMessage());
    }
  }

  private static Result<SemanticDefinition, SemanticError> decodeVocabEntry(byte[] payload) {
    ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
    String vocabularyId = readUtf8(buffer);
    String termPath = readUtf8(buffer);
    String version = readUtf8(buffer);
    String displayName = readUtf8(buffer);
    VocabRef ref = new VocabRef(vocabularyId, termPath, version);
    return Result.ok(new SemanticDefinition(ref, displayName, displayName));
  }

  /** Canonical encoder for tests and fixtures. */
  public static byte[] encode(Map<String, SemanticDefinition> extensions) {
    ByteBuffer buffer =
        ByteBuffer.allocate(estimateSize(extensions))
            .order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(RegistryExtensionTlv.MAGIC);
    buffer.put((byte) RegistryExtensionTlv.VERSION);
    TreeMap<String, SemanticDefinition> ordered = new TreeMap<>(extensions);
    for (SemanticDefinition definition : ordered.values()) {
      byte[] entryPayload = encodeVocabEntry(definition);
      buffer.putShort((short) RegistryExtensionTlv.TLV_VOCAB_ENTRY);
      buffer.putInt(entryPayload.length);
      buffer.put(entryPayload);
    }
    byte[] result = new byte[buffer.position()];
    buffer.flip();
    buffer.get(result);
    return result;
  }

  private static byte[] encodeVocabEntry(SemanticDefinition definition) {
    VocabRef ref = definition.ref();
    int size =
        4
            + utf8Size(ref.vocabularyId())
            + utf8Size(ref.termPath())
            + utf8Size(ref.version())
            + utf8Size(definition.displayName());
    ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    writeUtf8(buffer, ref.vocabularyId());
    writeUtf8(buffer, ref.termPath());
    writeUtf8(buffer, ref.version());
    writeUtf8(buffer, definition.displayName());
    return buffer.array();
  }

  private static int estimateSize(Map<String, SemanticDefinition> extensions) {
    int size = RegistryExtensionTlv.MAGIC.length + 1;
    for (SemanticDefinition definition : extensions.values()) {
      byte[] entry = encodeVocabEntry(definition);
      size += 6 + entry.length;
    }
    return size;
  }

  private static int utf8Size(String value) {
    return 4 + value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static void writeUtf8(ByteBuffer buffer, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    buffer.putInt(bytes.length);
    buffer.put(bytes);
  }

  private static String readUtf8(ByteBuffer buffer) {
    int length = buffer.getInt();
    if (length < 0 || buffer.remaining() < length) {
      throw new IllegalArgumentException("Invalid UTF-8 length in registry extension");
    }
    return new String(readBytes(buffer, length), StandardCharsets.UTF_8);
  }

  private static byte[] readBytes(ByteBuffer buffer, int length) {
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return bytes;
  }

  private static Result<Map<String, SemanticDefinition>, SemanticError> err(ErrorCode code, String msg) {
    return Result.err(new SemanticError(code, 0L, msg));
  }
}
