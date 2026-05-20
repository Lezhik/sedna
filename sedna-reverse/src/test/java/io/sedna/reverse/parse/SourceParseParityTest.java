package io.sedna.reverse.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SourceParseParityTest {

  private static final Path SPRING_DEMO =
      io.sedna.core.examples.ExamplesLayout.findProjectRoot(
              Path.of("..").toAbsolutePath().normalize(), "spring-demo")
          .orElseThrow();

  @Test
  void spoonAndJavaParserProduceIdenticalParsedProject() {
    var spoon = new SpoonSourceParseStep().parse(SPRING_DEMO);
    var javaParser = new JavaSourceParseStep().parse(SPRING_DEMO);
    assertTrue(spoon.isOk(), () -> String.valueOf(spoon.error()));
    assertTrue(javaParser.isOk(), () -> String.valueOf(javaParser.error()));
    assertEquals(javaParser.value().classesByName(), spoon.value().classesByName());
  }

  @Test
  void primaryStepUsesSpoonForSpringDemo() {
    var primary = PrimarySourceParseStep.spoonWithJavaParserFallback().parse(SPRING_DEMO);
    var spoon = new SpoonSourceParseStep().parse(SPRING_DEMO);
    assertTrue(primary.isOk());
    assertTrue(spoon.isOk());
    assertEquals(spoon.value().classesByName(), primary.value().classesByName());
  }
}
