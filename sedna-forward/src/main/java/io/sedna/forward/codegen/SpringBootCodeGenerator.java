package io.sedna.forward.codegen;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.sedna.core.Contract;
import io.sedna.core.GenomeNode;
import io.sedna.core.NodeKind;
import io.sedna.core.Result;
import io.sedna.core.SchemaRef;
import io.sedna.core.SemanticError;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.model.ExecutionPlan;
import io.sedna.forward.model.GeneratedProject;
import io.sedna.forward.util.SpringBootNaming;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;

/** Codegen for Spring Boot monolith graphs produced by general reverse profile. */
public final class SpringBootCodeGenerator {

  private static final MustacheFactory MUSTACHE = new DefaultMustacheFactory();

  private final LlmClient llmClient;

  public SpringBootCodeGenerator(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  public Result<GeneratedProject, SemanticError> generate(ExecutionPlan plan) {
    String basePackage =
        SpringBootNaming.resolveBasePackage(plan.graph())
            .orElseThrow(() -> new IllegalStateException("Missing SOURCE_PACKAGE constraint"));
    Map<String, String> files = new TreeMap<>();
    Map<String, Object> templateScope = Map.of("basePackage", basePackage);
    try {
      files.put(
          "build.gradle.kts",
          renderTemplate("templates/spring/build.gradle.kts.mustache", templateScope));
      files.put(
          "settings.gradle.kts",
          renderTemplate("templates/spring/settings.gradle.kts.mustache", templateScope));
      files.put(
          "src/main/resources/application.yml",
          renderTemplate("templates/spring/application.yml.mustache", templateScope));

      String appSimpleName = applicationSimpleName(basePackage);
      files.put(
          pathFor(basePackage + "." + appSimpleName),
          generateApplicationClass(basePackage, appSimpleName));

      ClassName serviceType = null;
      for (long nodeId : plan.orderedNodeIds()) {
        GenomeNode node =
            plan.graph().nodes().stream()
                .filter(n -> n.nodeId() == nodeId)
                .findFirst()
                .orElseThrow();
        String qualified = SpringBootNaming.qualifiedClassName(node, basePackage);
        switch (node.kind()) {
          case ENTITY -> files.put(pathFor(qualified), generateEntity(node, qualified));
          case SERVICE -> {
            String source = generateService(node, qualified);
            files.put(pathFor(qualified), source);
            serviceType = ClassName.bestGuess(qualified);
          }
          case CONTROLLER -> {
            if (serviceType == null) {
              serviceType = inferServiceType(plan, basePackage);
            }
            files.put(pathFor(qualified), generateController(node, qualified, serviceType));
          }
          default -> {
            return Result.err(
                SemanticError.global(
                    io.sedna.core.ErrorCode.NOT_IMPLEMENTED,
                    "Unsupported node kind for Spring Boot codegen: " + node.kind()));
          }
        }
      }
      return Result.ok(new GeneratedProject(files));
    } catch (IOException ex) {
      return Result.err(
          SemanticError.global(
              io.sedna.core.ErrorCode.INTERNAL, "Codegen failed: " + ex.getMessage()));
    }
  }

  private static ClassName inferServiceType(ExecutionPlan plan, String basePackage) {
    return plan.graph().nodes().stream()
        .filter(node -> node.kind() == NodeKind.SERVICE)
        .map(node -> ClassName.bestGuess(SpringBootNaming.qualifiedClassName(node, basePackage)))
        .findFirst()
        .orElseThrow();
  }

  private static String applicationSimpleName(String basePackage) {
    int lastDot = basePackage.lastIndexOf('.');
    String leaf = lastDot >= 0 ? basePackage.substring(lastDot + 1) : basePackage;
    return capitalize(leaf) + "Application";
  }

  private static String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  private static String pathFor(String qualifiedName) {
    return "src/main/java/" + qualifiedName.replace('.', '/') + ".java";
  }

  private String generateApplicationClass(String basePackage, String simpleName) throws IOException {
    ClassName springApp = ClassName.get("org.springframework.boot", "SpringApplication");
    ClassName appClass = ClassName.get(basePackage, simpleName);
    TypeSpec type =
        TypeSpec.classBuilder(appClass)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                ClassName.get("org.springframework.boot.autoconfigure", "SpringBootApplication"))
            .addMethod(
                MethodSpec.methodBuilder("main")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(String[].class, "args")
                    .addStatement("$T.run($T.class, args)", springApp, appClass)
                    .build())
            .build();
    return JavaFile.builder(basePackage, type).build().toString();
  }

  private String generateEntity(GenomeNode node, String qualified) throws IOException {
    int lastDot = qualified.lastIndexOf('.');
    String packageName = qualified.substring(0, lastDot);
    String simple = qualified.substring(lastDot + 1);
    TypeSpec type =
        TypeSpec.classBuilder(simple)
            .addModifiers(Modifier.PUBLIC)
            .addField(long.class, "id", Modifier.PRIVATE)
            .addField(String.class, "name", Modifier.PRIVATE)
            .build();
    return JavaFile.builder(packageName, type).build().toString();
  }

  private String generateService(GenomeNode node, String qualified) throws IOException {
    int lastDot = qualified.lastIndexOf('.');
    String packageName = qualified.substring(0, lastDot);
    String simple = qualified.substring(lastDot + 1);
    String signature = serviceMethodSignature(node);
    Result<String, SemanticError> bodyResult = llmClient.generateMethodBody(node, signature);
    if (!bodyResult.isOk()) {
      throw new IOException(bodyResult.error().message());
    }
    MethodSpec method =
        MethodSpec.methodBuilder(extractMethodName(signature))
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addCode(bodyResult.value() + "\n")
            .build();
    TypeSpec type =
        TypeSpec.classBuilder(simple)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
            .addMethod(method)
            .build();
    return JavaFile.builder(packageName, type).build().toString();
  }

  private String generateController(GenomeNode node, String qualified, ClassName serviceType)
      throws IOException {
    int lastDot = qualified.lastIndexOf('.');
    String packageName = qualified.substring(0, lastDot);
    String simple = qualified.substring(lastDot + 1);
    String serviceField = decapitalize(serviceType.simpleName());
    TypeSpec type =
        TypeSpec.classBuilder(simple)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
            .addAnnotation(ClassName.get("org.springframework.stereotype", "Controller"))
            .addField(serviceType, serviceField, Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(serviceType, serviceField)
                    .addStatement("this.$L = $L", serviceField, serviceField)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("handle")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(
                        AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                            .addMember("value", "$S", "/api/handle")
                            .build())
                    .returns(void.class)
                    .addStatement("$L.handle()", serviceField)
                    .build())
            .build();
    return JavaFile.builder(packageName, type).build().toString();
  }

  private static String extractMethodName(String signature) {
    int paren = signature.indexOf('(');
    if (paren < 0) {
      return "handle";
    }
    String head = signature.substring(0, paren).trim();
    int space = head.lastIndexOf(' ');
    return space >= 0 ? head.substring(space + 1) : head;
  }

  private static String decapitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  private static String serviceMethodSignature(GenomeNode node) {
    for (Contract contract : node.contracts()) {
      if (contract.ioSchema().format().equals(SchemaRef.JAVA_SIGNATURE)
          && !contract.ioSchema().payload().startsWith("class:")) {
        return contract.ioSchema().payload();
      }
    }
    return "void handle()";
  }

  private static String renderTemplate(String resource, Map<String, Object> scopes) throws IOException {
    try (InputStreamReader reader =
        new InputStreamReader(
            Objects.requireNonNull(
                SpringBootCodeGenerator.class.getClassLoader().getResourceAsStream(resource)),
            StandardCharsets.UTF_8)) {
      Mustache mustache = MUSTACHE.compile(reader, resource);
      StringWriter writer = new StringWriter();
      mustache.execute(writer, scopes).flush();
      return writer.toString();
    }
  }
}
