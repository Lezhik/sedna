package io.sedna.forward.llm;

import java.util.Locale;

/** Strips unsafe patterns from LLM-generated method bodies. */
final class LlmResponseSanitizer {

  private static final int MAX_BODY_LENGTH = 16_384;

  private LlmResponseSanitizer() {}

  static String sanitize(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String trimmed = raw.trim();
    if (trimmed.length() > MAX_BODY_LENGTH) {
      trimmed = trimmed.substring(0, MAX_BODY_LENGTH);
    }
    String[] forbidden = {
      "Runtime.getRuntime()",
      "ProcessBuilder",
      "exec(",
      "System.exit",
      "java.lang.Process",
      "bash ",
      "cmd.exe",
      "powershell"
    };
    String lower = trimmed.toLowerCase(Locale.ROOT);
    for (String pattern : forbidden) {
      if (lower.contains(pattern.toLowerCase(Locale.ROOT))) {
        return "";
      }
    }
    return trimmed;
  }
}
