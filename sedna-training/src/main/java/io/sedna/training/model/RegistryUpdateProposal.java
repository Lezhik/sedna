package io.sedna.training.model;

import io.sedna.core.VocabRef;
import java.util.Optional;

/**
 * Registry update proposal with deterministic conflict resolution.
 *
 * @param proposed vocabulary reference under review
 * @param resolution resolution action (e.g. {@code SKIP_EXACT_MATCH})
 * @param conflicting existing vocabulary reference when a conflict exists
 */
public record RegistryUpdateProposal(
    VocabRef proposed, String resolution, Optional<VocabRef> conflicting) {

  /** Normalizes null optional to empty. */
  public RegistryUpdateProposal {
    conflicting = conflicting == null ? Optional.empty() : conflicting;
  }
}
