package io.sedna.forward.util;

import io.sedna.core.GenomeNode;

/** Deterministic CMS class names for the reference fixture vocabulary. */
public final class CmsNaming {

  public static final String BASE_PACKAGE = "io.sedna.cms";

  private CmsNaming() {}

  public static String simpleClassName(GenomeNode node) {
    return switch (node.kind()) {
      case ENTITY -> "User";
      case SERVICE -> "UserService";
      case CONTROLLER -> "UserController";
      default -> node.kind().name() + node.nodeId();
    };
  }

  public static String subPackage(GenomeNode node) {
    return switch (node.kind()) {
      case ENTITY -> "domain";
      case SERVICE -> "service";
      case CONTROLLER -> "web";
      default -> "generated";
    };
  }

  public static String qualifiedClassName(GenomeNode node) {
    return BASE_PACKAGE + "." + subPackage(node) + "." + simpleClassName(node);
  }
}
