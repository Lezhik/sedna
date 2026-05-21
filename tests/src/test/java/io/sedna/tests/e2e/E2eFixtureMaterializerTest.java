package io.sedna.tests.e2e;

import org.junit.jupiter.api.Test;

/**
 * Writes committed binary fixtures under {@code tests/fixtures/}. Not tagged {@code e2e}.
 *
 * <p>Regenerate: {@code ./gradlew :tests:test --tests
 * io.sedna.tests.e2e.E2eFixtureMaterializerTest}
 */
class E2eFixtureMaterializerTest {

  @Test
  void materializeFixtureTree() throws Exception {
    E2eFixtures.materializeAllFixtures();
  }
}
