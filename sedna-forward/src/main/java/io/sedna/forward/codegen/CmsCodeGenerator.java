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
import io.sedna.core.Result;
import io.sedna.core.SchemaRef;
import io.sedna.core.SemanticError;
import io.sedna.forward.llm.LlmClient;
import io.sedna.forward.model.ExecutionPlan;
import io.sedna.forward.model.GeneratedProject;
import io.sedna.forward.util.CmsNaming;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;

/** Deterministic CMS Spring Boot codegen for the reference fixture profile. */
public final class CmsCodeGenerator {

  private static final MustacheFactory MUSTACHE = new DefaultMustacheFactory();
  private static final ClassName ANNOTATION_SERVICE =
      ClassName.get("org.springframework.stereotype", "Service");
  private static final ClassName ANNOTATION_REST_CONTROLLER =
      ClassName.get("org.springframework.web.bind.annotation", "RestController");
  private static final ClassName ANNOTATION_CONTROLLER =
      ClassName.get("org.springframework.stereotype", "Controller");
  private static final ClassName ANNOTATION_SPRING_BOOT_APPLICATION =
      ClassName.get("org.springframework.boot.autoconfigure", "SpringBootApplication");

  private final LlmClient llmClient;

  public CmsCodeGenerator(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  public Result<GeneratedProject, SemanticError> generate(ExecutionPlan plan) {
    Map<String, String> files = new TreeMap<>();
    try {
      files.put("build.gradle.kts", renderTemplate("templates/cms/build.gradle.kts.mustache", Map.of()));
      files.put("settings.gradle.kts", renderTemplate("templates/cms/settings.gradle.kts.mustache", Map.of()));
      files.put(
          "src/main/resources/application.yml",
          renderTemplate("templates/cms/application.yml.mustache", Map.of()));

      files.put(
          pathFor(CmsNaming.BASE_PACKAGE + ".CmsApplication"),
          generateApplicationClass());

      for (long nodeId : plan.orderedNodeIds()) {
        GenomeNode node =
            plan.graph().nodes().stream()
                .filter(n -> n.nodeId() == nodeId)
                .findFirst()
                .orElseThrow();
        switch (node.kind()) {
          case ENTITY -> files.put(pathFor(CmsNaming.qualifiedClassName(node)), generateEntity(node));
          case SERVICE -> files.put(pathFor(CmsNaming.qualifiedClassName(node)), generateService(node));
          case CONTROLLER -> files.put(pathFor(CmsNaming.qualifiedClassName(node)), generateController(node));
          default -> {
            return Result.err(
                io.sedna.core.SemanticError.global(
                    io.sedna.core.ErrorCode.NOT_IMPLEMENTED,
                    "Unsupported node kind for CMS codegen: " + node.kind()));
          }
        }
      }

      return Result.ok(new GeneratedProject(files));
    } catch (IOException ex) {
      return Result.err(
          io.sedna.core.SemanticError.global(
              io.sedna.core.ErrorCode.INTERNAL, "Codegen failed: " + ex.getMessage()));
    }
  }

  private static String pathFor(String qualifiedName) {
    return "src/main/java/" + qualifiedName.replace('.', '/') + ".java";
  }

  private String generateApplicationClass() throws IOException {
    ClassName springApp = ClassName.get("org.springframework.boot", "SpringApplication");
    ClassName appClass = ClassName.get(CmsNaming.BASE_PACKAGE, "CmsApplication");
    TypeSpec type =
        TypeSpec.classBuilder(appClass)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ANNOTATION_SPRING_BOOT_APPLICATION)
            .addMethod(
                MethodSpec.methodBuilder("main")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(String[].class, "args")
                    .addStatement("$T.run($T.class, args)", springApp, appClass)
                    .build())
            .build();
    return JavaFile.builder(CmsNaming.BASE_PACKAGE, type).build().toString();
  }

  private String generateEntity(GenomeNode node) throws IOException {
    TypeSpec type =
        TypeSpec.classBuilder(CmsNaming.simpleClassName(node))
            .addModifiers(Modifier.PUBLIC)
            .addField(long.class, "id", Modifier.PRIVATE)
            .addField(String.class, "name", Modifier.PRIVATE)
            .addMethod(
                MethodSpec.methodBuilder("getId")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(long.class)
                    .addStatement("return id")
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("getName")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("return name")
                    .build())
            .build();
    return JavaFile.builder(CmsNaming.BASE_PACKAGE + ".domain", type).build().toString();
  }

  private Result<String, SemanticError> methodBody(GenomeNode node, String signature) {
    return llmClient.generateMethodBody(node, signature);
  }

  private String generateService(GenomeNode node) throws IOException {
    String signature = serviceMethodSignature(node);
    Result<String, SemanticError> bodyResult = methodBody(node, signature);
    if (!bodyResult.isOk()) {
      throw new IOException(bodyResult.error().message());
    }
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("handle")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addCode(bodyResult.value() + "\n");

    TypeSpec type =
        TypeSpec.classBuilder(CmsNaming.simpleClassName(node))
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ANNOTATION_SERVICE)
            .addMethod(method.build())
            .build();
    return JavaFile.builder(CmsNaming.BASE_PACKAGE + ".service", type).build().toString();
  }

  private static String serviceMethodSignature(GenomeNode node) {
    for (Contract contract : node.contracts()) {
      if (contract.ioSchema().format().equals(SchemaRef.JAVA_SIGNATURE)) {
        return contract.ioSchema().payload();
      }
    }
    return "void handle()";
  }

  private String generateController(GenomeNode node) throws IOException {
    ClassName serviceType = ClassName.get(CmsNaming.BASE_PACKAGE + ".service", "UserService");
    TypeSpec type =
        TypeSpec.classBuilder(CmsNaming.simpleClassName(node))
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ANNOTATION_REST_CONTROLLER)
            .addAnnotation(ANNOTATION_CONTROLLER)
            .addField(serviceType, "userService", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(serviceType, "userService")
                    .addStatement("this.userService = userService")
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("handle")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(
                        AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                            .addMember("value", "$S", "/users/handle")
                            .build())
                    .returns(void.class)
                    .addStatement("userService.handle()")
                    .build())
            .build();
    return JavaFile.builder(CmsNaming.BASE_PACKAGE + ".web", type).build().toString();
  }

  private static String renderTemplate(String resource, Map<String, Object> scopes) throws IOException {
    try (InputStreamReader reader =
        new InputStreamReader(
            Objects.requireNonNull(
                CmsCodeGenerator.class.getClassLoader().getResourceAsStream(resource)),
            StandardCharsets.UTF_8)) {
      Mustache mustache = MUSTACHE.compile(reader, resource);
      StringWriter writer = new StringWriter();
      mustache.execute(writer, scopes).flush();
      return writer.toString();
    }
  }
}
