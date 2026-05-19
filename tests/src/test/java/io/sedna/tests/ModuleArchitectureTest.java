package io.sedna.tests;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModuleArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes = new ClassFileImporter().importPackages("io.sedna");
  }

  @Test
  void forwardAndReverseDoNotDependOnEachOther() {
    noClasses()
        .that()
        .resideInAPackage("io.sedna.forward..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.sedna.reverse..")
        .check(classes);

    noClasses()
        .that()
        .resideInAPackage("io.sedna.reverse..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.sedna.forward..")
        .check(classes);
  }

  @Test
  void pipelineModulesDoNotUseHashMapInDeterministicLayers() {
    String[] deterministicPackages = {
      "io.sedna.dna..", "io.sedna.forward..", "io.sedna.runtime.."
    };
    for (String pkg : deterministicPackages) {
      noClasses()
          .that()
          .resideInAPackage(pkg)
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.HashMap")
          .check(classes);
      noClasses()
          .that()
          .resideInAPackage(pkg)
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.HashSet")
          .check(classes);
    }
  }

  @Test
  void mutationAndTrainingDoNotDependOnForward() {
    noClasses()
        .that()
        .resideInAnyPackage("io.sedna.mutation..", "io.sedna.training..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.sedna.forward..")
        .check(classes);
  }

  @Test
  void canonicalGraphDtosDefinedOnlyInCore() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleName("SemanticGraph")
            .or()
            .haveSimpleName("GenomeNode")
            .or()
            .haveSimpleName("SemanticLink")
            .should()
            .resideInAPackage("io.sedna.core..");
    rule.check(classes);
  }
}
