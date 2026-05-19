package io.sedna.tests;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Production modules must not invoke dynamic bytecode execution APIs. */
class NoDynamicBytecodeArchTest {

  private static final JavaClasses PRODUCTION =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(
              "io.sedna.core",
              "io.sedna.dna",
              "io.sedna.registry",
              "io.sedna.validation",
              "io.sedna.forward",
              "io.sedna.reverse",
              "io.sedna.runtime",
              "io.sedna.mutation",
              "io.sedna.training",
              "io.sedna.persistence",
              "io.sedna.cli");

  @Test
  void noRuntimeExecOrProcessBuilderInProductionCode() {
    noClasses()
        .should()
        .callMethod(Runtime.class, "exec", String.class)
        .orShould()
        .callConstructor(ProcessBuilder.class, String.class)
        .check(PRODUCTION);
  }
}
