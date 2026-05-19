package io.sedna.training.model;

import java.util.List;

/** Training artifacts for a single project folder. */
public record TrainingProjectResult(
    String projectPath,
    SemanticTrajectory trajectory,
    List<SemanticEmbedding> embeddings,
    List<MutationDatasetEntry> mutationDataset,
    List<RegistryUpdateProposal> registryProposals) {
  public TrainingProjectResult {
    embeddings = List.copyOf(embeddings);
    mutationDataset = List.copyOf(mutationDataset);
    registryProposals = List.copyOf(registryProposals);
  }
}
