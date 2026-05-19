package io.sedna.core;

import java.util.Optional;

/**
 * Request to mutate a subtree rooted at {@code targetNodeId}.
 *
 * <p>Operand fields are used depending on {@link MutationType}; unused operands must be empty.
 */
public record Mutation(
    long targetNodeId,
    MutationType operation,
    Optional<GenomeNode> insertNode,
    Optional<SemanticGraph> replacementSubtree,
    Optional<Contract> contractUpgrade,
    Optional<Constraint> injectedConstraint) {

  public Mutation {
    insertNode = insertNode == null ? Optional.empty() : insertNode;
    replacementSubtree = replacementSubtree == null ? Optional.empty() : replacementSubtree;
    contractUpgrade = contractUpgrade == null ? Optional.empty() : contractUpgrade;
    injectedConstraint = injectedConstraint == null ? Optional.empty() : injectedConstraint;
  }

  public Mutation(long targetNodeId, MutationType operation) {
    this(targetNodeId, operation, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }
}
