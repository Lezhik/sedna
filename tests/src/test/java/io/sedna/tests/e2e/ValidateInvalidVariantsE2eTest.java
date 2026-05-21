package io.sedna.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sedna.core.ErrorCode;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** E2E-013 extended — invalid fixture variants reject with stable errors. */
@Tag("e2e")
class ValidateInvalidVariantsE2eTest {

  @BeforeAll
  static void materialize() throws Exception {
    E2eFixtures.materializeAllFixtures();
  }

  static Stream<Arguments> invalidFixtures() {
    return Stream.of(
        Arguments.of(E2eFixtures.invalidOrphanLinkPath(), ErrorCode.VALIDATION_FAILED),
        Arguments.of(E2eFixtures.invalidDuplicateNodePath(), ErrorCode.VALIDATION_FAILED),
        Arguments.of(E2eFixtures.invalidUnknownVocabPath(), ErrorCode.UNKNOWN_VOCAB),
        Arguments.of(E2eFixtures.invalidDnaMagicPath(), ErrorCode.INVALID_DNA));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFixtures")
  void validateRejectsDeterministically(Path fixture, ErrorCode expectedCode) throws Exception {
    E2eTestSupport.CliResult first =
        E2eTestSupport.runCli("validate", "--input=" + fixture, "--format=json");
    E2eTestSupport.CliResult second =
        E2eTestSupport.runCli("validate", "--input=" + fixture, "--format=json");
    assertEquals(1, first.exitCode(), () -> first.stdout());
    assertEquals(first.exitCode(), second.exitCode());
    assertTrue(
        first.stdout().contains(expectedCode.name()),
        () -> "expected " + expectedCode + " in: " + first.stdout());
    assertEquals(normalize(first.stdout()), normalize(second.stdout()));
  }

  private static String normalize(String text) {
    return text.replace("\r\n", "\n").trim();
  }
}
