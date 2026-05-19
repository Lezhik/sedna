package io.sedna.runtime.state;

import io.sedna.core.NodeKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Deterministic FSM state labels for STATEFUL profile (no external state machine). */
public final class FsmStateTracker {

  private static final HexFormat HEX = HexFormat.of();

  private String state = "INIT";

  public String state() {
    return state;
  }

  public String enterNode(long nodeId, NodeKind kind) {
    state = "EXECUTING:" + nodeId + ":" + kind.name();
    return state;
  }

  public String completeNode(long nodeId) {
    state = "DONE:" + nodeId;
    return state;
  }

  public String stateHash() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(state.getBytes(StandardCharsets.UTF_8));
      return HEX.formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void restore(String savedState) {
    state = savedState == null || savedState.isBlank() ? "INIT" : savedState;
  }
}
