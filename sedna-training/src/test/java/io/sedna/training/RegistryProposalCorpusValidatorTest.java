package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.VocabRef;
import io.sedna.training.model.RegistryUpdateProposal;
import io.sedna.training.model.SemanticTrajectory;
import io.sedna.training.model.TrainingProjectResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegistryProposalCorpusValidatorTest {

  private final RegistryProposalCorpusValidator validator = new RegistryProposalCorpusValidator();

  @Test
  void identicalProposalsAcrossProjectsPass() {
    VocabRef ref = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
    var proposal =
        new RegistryUpdateProposal(ref, RegistryUpdateProposer.SKIP_EXACT, Optional.empty());
    var a = dummyProject("/a", List.of(proposal));
    var b = dummyProject("/b", List.of(proposal));
    assertTrue(validator.validateCorpus(List.of(a, b)).isOk());
  }

  @Test
  void conflictingResolutionsFail() {
    VocabRef ref = new VocabRef("core", "CUSTOM.TERM", "v1");
    var skip = new RegistryUpdateProposal(ref, RegistryUpdateProposer.SKIP_EXACT, Optional.empty());
    var append =
        new RegistryUpdateProposal(ref, RegistryUpdateProposer.APPEND_VERSION, Optional.empty());
    var a = dummyProject("/a", List.of(skip));
    var b = dummyProject("/b", List.of(append));
    assertFalse(validator.validateCorpus(List.of(a, b)).isOk());
  }

  @Test
  void unknownResolutionCodeFails() {
    VocabRef ref = new VocabRef("core", "CUSTOM.TERM2", "v1");
    var bad = new RegistryUpdateProposal(ref, "MERGE_LATER", Optional.empty());
    assertFalse(validator.validateCorpus(List.of(dummyProject("/x", List.of(bad)))).isOk());
  }

  private static TrainingProjectResult dummyProject(
      String root, List<RegistryUpdateProposal> proposals) {
    return new TrainingProjectResult(
        root,
        new SemanticTrajectory(root, List.of(), List.of(), List.of()),
        List.of(),
        List.of(),
        proposals);
  }
}
