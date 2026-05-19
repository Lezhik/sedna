package io.sedna.forward.util;

/** Deterministic capability version matching. */
public final class CapabilityVersionMatcher {

  private CapabilityVersionMatcher() {}

  public static boolean satisfies(String requiredConstraint, String providedVersion) {
    if (requiredConstraint.startsWith(">=")) {
      String min = requiredConstraint.substring(2).trim();
      return compareVersions(providedVersion, min) >= 0;
    }
    return requiredConstraint.equals(providedVersion);
  }

  private static int compareVersions(String left, String right) {
    String[] leftParts = left.split("\\.");
    String[] rightParts = right.split("\\.");
    int length = Math.max(leftParts.length, rightParts.length);
    for (int i = 0; i < length; i++) {
      int l = i < leftParts.length ? parsePart(leftParts[i]) : 0;
      int r = i < rightParts.length ? parsePart(rightParts[i]) : 0;
      if (l != r) {
        return Integer.compare(l, r);
      }
    }
    return 0;
  }

  private static int parsePart(String part) {
    int end = 0;
    while (end < part.length() && Character.isDigit(part.charAt(end))) {
      end++;
    }
    if (end == 0) {
      return 0;
    }
    return Integer.parseInt(part.substring(0, end));
  }
}
