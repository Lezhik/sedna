package io.sedna.training.model;

import io.sedna.core.VocabRef;
import java.util.Optional;

/** Registry update proposal with deterministic conflict resolution. */
public record RegistryUpdateProposal(
    VocabRef proposed, String resolution, Optional<VocabRef> conflicting) {
  public RegistryUpdateProposal {
    conflicting = conflicting == null ? Optional.empty() : conflicting;
  }
}
