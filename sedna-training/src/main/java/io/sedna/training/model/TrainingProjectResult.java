package io.sedna.training.model;

import java.util.List;

/**
 * Training artifacts for a single project folder.
 *
 * @param projectPath absolute project path
 * @param trajectory semantic trajectory for the project
 * @param embeddings deterministic embeddings for HEAD graph nodes
 * @param mutationDataset labeled mutation candidates
 * @param registryProposals vocabulary registry update proposals
 */
public record TrainingProjectResult(
    String projectPath,
    SemanticTrajectory trajectory,
    List<SemanticEmbedding> embeddings,
    List<MutationDatasetEntry> mutationDataset,
    List<RegistryUpdateProposal> registryProposals) {

  /** Defensive copy of list components. */
  public TrainingProjectResult {
    embeddings = List.copyOf(embeddings);
    mutationDataset = List.copyOf(mutationDataset);
    registryProposals = List.copyOf(registryProposals);
  }
}
