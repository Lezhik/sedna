package io.sedna.reverse.parse;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.ParsedProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/** Step 1 — Spoon AST parser (primary). */
public final class SpoonSourceParseStep implements SourceParseStep {

  @Override
  public Result<ParsedProject, SemanticError> parse(Path projectRoot) {
    Path sourceRoot = projectRoot.resolve("src/main/java");
    if (!Files.isDirectory(sourceRoot)) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Missing src/main/java in " + projectRoot));
    }

    try {
      Launcher launcher = new Launcher();
      launcher.addInputResource(sourceRoot.toString());
      launcher.getEnvironment().setComplianceLevel(21);
      launcher.getEnvironment().setNoClasspath(true);
      launcher.getEnvironment().setCommentEnabled(false);
      launcher.buildModel();

      Map<Path, List<CtType<?>>> typesByFile = new TreeMap<>();
      for (CtType<?> type : launcher.getModel().getAllTypes()) {
        if (!type.isTopLevel() || type.isAnonymous()) {
          continue;
        }
        if (!type.getPosition().isValidPosition()) {
          continue;
        }
        Path file = Path.of(type.getPosition().getFile().getPath()).toAbsolutePath().normalize();
        if (!file.startsWith(sourceRoot.toAbsolutePath().normalize())) {
          continue;
        }
        typesByFile.computeIfAbsent(file, ignored -> new ArrayList<>()).add(type);
      }

      Map<String, ParsedClass> classes = new LinkedHashMap<>();
      try (Stream<Path> paths = Files.walk(sourceRoot)) {
        List<Path> javaFiles =
            paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        for (Path file : javaFiles) {
          Path absoluteFile = file.toAbsolutePath().normalize();
          List<CtType<?>> types = typesByFile.getOrDefault(absoluteFile, List.of());
          if (types.isEmpty()) {
            return Result.err(
                SemanticError.global(ErrorCode.VALIDATION_FAILED, "No type in file: " + file));
          }
          if (types.size() > 1) {
            return Result.err(
                SemanticError.global(
                    ErrorCode.VALIDATION_FAILED, "Multiple top-level types not supported: " + file));
          }
          ParsedClass parsed = toParsedClass(types.getFirst());
          classes.put(parsed.qualifiedName(), parsed);
        }
      }

      if (classes.isEmpty()) {
        return Result.err(SemanticError.global(ErrorCode.VALIDATION_FAILED, "No Java sources found"));
      }

      ParsedProject raw = new ParsedProject(projectRoot.toAbsolutePath().normalize(), classes);
      return Result.ok(ParsedClassAssembler.resolveDependencies(raw));
    } catch (IOException | RuntimeException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  private static ParsedClass toParsedClass(CtType<?> type) {
    String packageName = type.getPackage() != null ? type.getPackage().getQualifiedName() : "";
    String qualifiedName = type.getQualifiedName();

    List<String> annotations =
        type.getAnnotations().stream()
            .map(ann -> annotationSimpleName(ann.getAnnotationType().getQualifiedName()))
            .sorted()
            .distinct()
            .toList();

    List<String> dependencies = new ArrayList<>();
    type.getFields().forEach(field -> collectTypeDependency(field.getType(), dependencies));
    if (type instanceof CtClass<?> clazz) {
      for (CtConstructor<?> ctor : clazz.getConstructors()) {
        ctor.getParameters().forEach(param -> collectTypeDependency(param.getType(), dependencies));
      }
    }

    List<String> methods =
        type.getMethods().stream()
            .filter(method -> method.isPublic() && !method.isStatic())
            .map(SpoonSourceParseStep::methodSignature)
            .sorted()
            .toList();

    List<String> simpleDependencies = dependencies.stream().distinct().sorted().toList();

    return new ParsedClass(
        qualifiedName,
        packageName,
        type.getSimpleName(),
        annotations,
        simpleDependencies,
        methods);
  }

  private static String methodSignature(CtMethod<?> method) {
    String params =
        method.getParameters().stream()
            .map(param -> param.getType().getSimpleName())
            .reduce((a, b) -> a + "," + b)
            .map(value -> "(" + value + ")")
            .orElse("()");
    return method.getType().getSimpleName() + " " + method.getSimpleName() + params;
  }

  private static void collectTypeDependency(CtTypeReference<?> typeRef, List<String> dependencies) {
    if (typeRef == null || typeRef.isPrimitive()) {
      return;
    }
    String simple = typeRef.getSimpleName();
    if (simple != null && !simple.isBlank()) {
      dependencies.add(simple);
    }
  }

  private static String annotationSimpleName(String qualified) {
    int dot = qualified.lastIndexOf('.');
    return dot >= 0 ? qualified.substring(dot + 1) : qualified;
  }
}
