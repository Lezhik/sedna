package io.sedna.reverse.parse;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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

/** Step 1 — parse Java sources (JavaParser; Spoon deferred for generalized projects). */
public final class JavaSourceParseStep {

  public Result<ParsedProject, SemanticError> parse(Path projectRoot) {
    Path sourceRoot = projectRoot.resolve("src/main/java");
    if (!Files.isDirectory(sourceRoot)) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "Missing src/main/java in " + projectRoot));
    }

    Map<String, ParsedClass> classes = new LinkedHashMap<>();
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      List<Path> javaFiles =
          paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
      for (Path file : javaFiles) {
        Result<ParsedClass, SemanticError> parsed = parseFile(file, sourceRoot);
        if (!parsed.isOk()) {
          return Result.err(parsed.error());
        }
        classes.put(parsed.value().qualifiedName(), parsed.value());
      }
    } catch (IOException ex) {
      return Result.err(SemanticError.global(ErrorCode.INTERNAL, ex.getMessage()));
    }

    if (classes.isEmpty()) {
      return Result.err(SemanticError.global(ErrorCode.VALIDATION_FAILED, "No Java sources found"));
    }

    ParsedProject raw = new ParsedProject(projectRoot.toAbsolutePath().normalize(), classes);
    return Result.ok(resolveDependencies(raw));
  }

  private static ParsedProject resolveDependencies(ParsedProject project) {
    Map<String, String> simpleToQualified = new TreeMap<>();
    for (ParsedClass parsed : project.classes()) {
      simpleToQualified.put(parsed.simpleName(), parsed.qualifiedName());
    }

    Map<String, ParsedClass> resolved = new TreeMap<>();
    for (ParsedClass parsed : project.classes()) {
      List<String> dependencies =
          parsed.dependencyQualifiedNames().stream()
              .map(name -> resolveDependencyName(name, simpleToQualified, project.classesByName()))
              .filter(project.classesByName()::containsKey)
              .distinct()
              .sorted()
              .toList();
      resolved.put(
          parsed.qualifiedName(),
          new ParsedClass(
              parsed.qualifiedName(),
              parsed.packageName(),
              parsed.simpleName(),
              parsed.annotationSimpleNames(),
              dependencies,
              parsed.publicMethodSignatures()));
    }
    return new ParsedProject(project.projectRoot(), resolved);
  }

  private static String resolveDependencyName(
      String dependency, Map<String, String> simpleToQualified, Map<String, ParsedClass> classesByName) {
    if (classesByName.containsKey(dependency)) {
      return dependency;
    }
    String qualified = simpleToQualified.get(dependency);
    return qualified != null ? qualified : dependency;
  }

  private static Result<ParsedClass, SemanticError> parseFile(Path file, Path sourceRoot)
      throws IOException {
    CompilationUnit unit = StaticJavaParser.parse(file);
    List<ClassOrInterfaceDeclaration> types = unit.findAll(ClassOrInterfaceDeclaration.class);
    if (types.isEmpty()) {
      return Result.err(
          SemanticError.global(ErrorCode.VALIDATION_FAILED, "No type in file: " + file));
    }
    if (types.size() > 1) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "Multiple top-level types not supported: " + file));
    }

    ClassOrInterfaceDeclaration type = types.getFirst();
    String packageName = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
    String qualifiedName =
        packageName.isBlank() ? type.getNameAsString() : packageName + "." + type.getNameAsString();

    List<String> annotations =
        type.getAnnotations().stream()
            .map(JavaSourceParseStep::annotationSimpleName)
            .sorted()
            .distinct()
            .toList();

    List<String> dependencies = new ArrayList<>();
    type.getFields().forEach(field -> collectTypeDependencies(field, dependencies));
    type.getConstructors()
        .forEach(
            ctor ->
                ctor.getParameters()
                    .forEach(
                        param -> {
                          if (param.getType() instanceof ClassOrInterfaceType classType) {
                            dependencies.add(classType.getNameAsString());
                          }
                        }));

    List<String> methods =
        type.getMethods().stream()
            .filter(method -> method.isPublic() && !method.isStatic())
            .map(
                method ->
                    method.getTypeAsString()
                        + " "
                        + method.getNameAsString()
                        + method.getParameters().stream()
                            .map(p -> p.getTypeAsString())
                            .reduce((a, b) -> a + "," + b)
                            .map(params -> "(" + params + ")")
                            .orElse("()"))
            .sorted()
            .toList();

    List<String> simpleDependencies = dependencies.stream().distinct().sorted().toList();

    return Result.ok(
        new ParsedClass(
            qualifiedName, packageName, type.getNameAsString(), annotations, simpleDependencies, methods));
  }

  private static void collectTypeDependencies(FieldDeclaration field, List<String> dependencies) {
    field.getVariables()
        .forEach(
            variable -> {
              if (variable.getType() instanceof ClassOrInterfaceType classType) {
                dependencies.add(classType.getNameAsString());
              }
            });
  }

  private static String annotationSimpleName(AnnotationExpr annotation) {
    String name = annotation.getNameAsString();
    int dot = name.lastIndexOf('.');
    return dot >= 0 ? name.substring(dot + 1) : name;
  }
}
