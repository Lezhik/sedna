package io.sedna.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.dna.DnaServices;
import io.sedna.dna.SednaFoldMotifCodec;
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.reverse.cms.CmsSemanticRules;
import io.sedna.reverse.spring.SpringBootSemanticRules;
import io.sedna.validation.SemanticEquivalenceChecker;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Phase 10 P1: equivalence parameterized by detected reverse profile. */
class ProfileEquivalenceSuiteTest {

  enum ProjectProfile {
    CMS_REFERENCE,
    SPRING_BOOT_MONOLITH
  }

  static Stream<Arguments> fixtures() {
    return Stream.of(
        Arguments.of(ProjectProfile.CMS_REFERENCE, "cms-reference"),
        Arguments.of(ProjectProfile.SPRING_BOOT_MONOLITH, "spring-demo"),
        Arguments.of(ProjectProfile.SPRING_BOOT_MONOLITH, "inventory-demo"),
        Arguments.of(ProjectProfile.SPRING_BOOT_MONOLITH, "order-demo"));
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void reverseForwardEquivalenceMatchesProfile(
      ProjectProfile expectedProfile, String fixtureName, @TempDir Path outputDir) {
    Path project = RepoPaths.exampleProject(fixtureName);

    var reverse = ReverseServices.pipeline();
    var structural = reverse.buildStructuralGraph(project);
    assertTrue(structural.isOk(), () -> fixtureName + ": " + structural.error());
    assertProfile(expectedProfile, structural.value().project().projectRoot(), structural);

    var original = reverse.reverseGraph(project);
    assertTrue(original.isOk(), () -> fixtureName + ": " + original.error());

    byte[] dna = DnaServices.encoder().encode(original.value()).value();
    var forward =
        ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
    var written = forward.runToDirectory(dna, outputDir.resolve(fixtureName));
    assertTrue(written.isOk(), () -> fixtureName + ": " + written.error());

    var roundTrip = reverse.reverseGraph(outputDir.resolve(fixtureName));
    assertTrue(roundTrip.isOk(), () -> fixtureName + ": " + roundTrip.error());

    var expandedOriginal = SednaFoldMotifCodec.INSTANCE.expand(original.value());
    var expandedRoundTrip = SednaFoldMotifCodec.INSTANCE.expand(roundTrip.value());
    assertTrue(expandedOriginal.isOk());
    assertTrue(expandedRoundTrip.isOk());

    var equivalent =
        SemanticEquivalenceChecker.checkEquivalent(
            expandedOriginal.value(), expandedRoundTrip.value());
    assertTrue(equivalent.isOk(), () -> fixtureName + ": " + equivalent.error());
  }

  private static void assertProfile(
      ProjectProfile expectedProfile,
      Path projectRoot,
      io.sedna.core.Result<io.sedna.reverse.model.StructuralGraph, io.sedna.core.SemanticError>
          structural) {
    boolean cms = CmsSemanticRules.isCmsReference(structural.value());
    boolean spring = SpringBootSemanticRules.isSpringBootMonolith(structural.value());
    switch (expectedProfile) {
      case CMS_REFERENCE -> assertTrue(cms, () -> "Expected CMS profile for " + projectRoot);
      case SPRING_BOOT_MONOLITH ->
          assertTrue(spring && !cms, () -> "Expected Spring Boot profile for " + projectRoot);
    }
  }
}
