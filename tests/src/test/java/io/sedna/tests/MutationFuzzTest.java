package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.Constraint;
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.mutation.DefaultMutationEngine;
import io.sedna.mutation.MutationServices;
import io.sedna.training.MutationDatasetGenerator;
import java.util.Optional;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/** Fuzz-style mutation application must always return Result (commit or rollback). */
class MutationFuzzTest {

  private final DefaultMutationEngine engine = (DefaultMutationEngine) MutationServices.engine();
  private final MutationDatasetGenerator generator = new MutationDatasetGenerator();

  @RepeatedTest(24)
  void mutationDatasetEntriesNeverThrow(RepetitionInfo repetition) {
    int iteration = repetition.getCurrentRepetition();
    var graph = CmsReferenceFixtureGraph.create();
    var entries = generator.generate(graph);
    assertTrue(!entries.isEmpty());
    var entry = entries.get(iteration % entries.size());
    Mutation mutation =
        new Mutation(
            entry.targetNodeId(),
            entry.operation(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            entry.operation() == MutationType.CONSTRAINT_INJECTION
                ? Optional.of(new Constraint(entry.label()))
                : Optional.empty());
    var result = engine.apply(graph, mutation);
    assertTrue(result.isOk(), () -> String.valueOf(result.error()));
  }
}
