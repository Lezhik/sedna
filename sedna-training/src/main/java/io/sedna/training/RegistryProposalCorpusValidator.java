package io.sedna.training;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.training.model.RegistryUpdateProposal;
import io.sedna.training.model.TrainingProjectResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Validates registry update proposals across many training projects: same {@link
 * io.sedna.core.VocabRef} canonical key must resolve identically everywhere (deterministic corpus
 * merge).
 */
public final class RegistryProposalCorpusValidator {

  private static final Comparator<RegistryUpdateProposal> BY_KEY_THEN_RESOLUTION =
      Comparator.comparing((RegistryUpdateProposal p) -> p.proposed().canonicalKey())
          .thenComparing(RegistryUpdateProposal::resolution);

  /** Creates a corpus-level registry proposal validator. */
  public RegistryProposalCorpusValidator() {}

  /**
   * Ensures registry proposals resolve consistently across all projects.
   *
   * @param projects training results for each project
   * @return {@code true} when valid or structured error on conflict
   */
  public Result<Boolean, SemanticError> validateCorpus(List<TrainingProjectResult> projects) {
    List<RegistryUpdateProposal> flat = new ArrayList<>();
    for (TrainingProjectResult project : projects) {
      flat.addAll(project.registryProposals());
    }
    flat.sort(BY_KEY_THEN_RESOLUTION);

    TreeMap<String, RegistryUpdateProposal> firstByKey = new TreeMap<>();
    for (RegistryUpdateProposal proposal : flat) {
      if (!isKnownResolution(proposal.resolution())) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "Unknown registry proposal resolution: "
                    + proposal.resolution()
                    + " for "
                    + proposal.proposed().canonicalKey()));
      }
      String key = proposal.proposed().canonicalKey();
      RegistryUpdateProposal existing = firstByKey.putIfAbsent(key, proposal);
      if (existing != null && !existing.equals(proposal)) {
        return Result.err(
            SemanticError.global(
                ErrorCode.VALIDATION_FAILED,
                "Registry proposal conflict for "
                    + key
                    + ": "
                    + existing.resolution()
                    + " vs "
                    + proposal.resolution()));
      }
    }
    return Result.ok(Boolean.TRUE);
  }

  private static boolean isKnownResolution(String resolution) {
    return RegistryUpdateProposer.SKIP_EXACT.equals(resolution)
        || RegistryUpdateProposer.APPEND_VERSION.equals(resolution)
        || RegistryUpdateProposer.MANUAL_REVIEW.equals(resolution);
  }
}
