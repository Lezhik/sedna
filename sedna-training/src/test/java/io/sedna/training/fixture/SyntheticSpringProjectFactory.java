package io.sedna.training.fixture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/** Deterministic minimal Spring Boot monolith for training acceptance fixtures. */
public final class SyntheticSpringProjectFactory {

  public static final int PHASE13_MIN_COMMITS = 10;
  public static final int PHASE13_MIN_PROJECTS = 20;
  public static final int PHASE13_MIN_MUTATION_ROWS = 500;

  private SyntheticSpringProjectFactory() {}

  public static Path create(Path projectRoot, String basePackage) throws IOException {
    String pkgPath = basePackage.replace('.', '/');
    Files.createDirectories(projectRoot.resolve("src/main/java/" + pkgPath + "/domain"));
    Files.createDirectories(projectRoot.resolve("src/main/java/" + pkgPath + "/service"));
    Files.createDirectories(projectRoot.resolve("src/main/java/" + pkgPath + "/web"));

    String appSimple = simpleNameFromPackage(basePackage) + "Application";
    Files.writeString(
        projectRoot.resolve("src/main/java/" + pkgPath + "/" + appSimple + ".java"),
        """
        package %s;%n\
        import org.springframework.boot.autoconfigure.SpringBootApplication;%n\
        @SpringBootApplication%n\
        public class %s {}%n\
        """
            .formatted(basePackage, appSimple));
    Files.writeString(
        projectRoot.resolve("src/main/java/" + pkgPath + "/domain/Item.java"),
        "package %s.domain;%npublic class Item { private long id; }%n".formatted(basePackage));
    writeService(projectRoot, pkgPath, basePackage, "public void handle() {}");
    Files.writeString(
        projectRoot.resolve("src/main/java/" + pkgPath + "/web/ItemController.java"),
        """
        package %s.web;%n\
        import %s.service.ItemService;%n\
        import org.springframework.web.bind.annotation.RestController;%n\
        @RestController%n\
        public class ItemController {%n\
          private final ItemService itemService;%n\
          public ItemController(ItemService itemService) { this.itemService = itemService; }%n\
          public void handle() { itemService.handle(); }%n\
        }%n\
        """
            .formatted(basePackage, basePackage));
    Files.writeString(
        projectRoot.resolve("build.gradle.kts"), "plugins { java }" + System.lineSeparator());
    return projectRoot;
  }

  public static void initGitHistory(Path projectRoot, String basePackage, int commitCount)
      throws IOException, GitAPIException {
    if (commitCount < 1) {
      throw new IllegalArgumentException("commitCount must be >= 1");
    }
    String pkgPath = basePackage.replace('.', '/');
    try (Git git = Git.init().setDirectory(projectRoot.toFile()).call()) {
      git.add().addFilepattern(".").call();
      git.commit().setMessage("commit-0").call();
      for (int i = 1; i < commitCount; i++) {
        String signature = serviceSignature(i);
        writeService(projectRoot, pkgPath, basePackage, signature);
        git.add().addFilepattern(".").call();
        git.commit().setMessage("commit-" + i).call();
      }
    }
  }

  private static void writeService(
      Path projectRoot, String pkgPath, String basePackage, String methodBody)
      throws IOException {
    Files.writeString(
        projectRoot.resolve("src/main/java/" + pkgPath + "/service/ItemService.java"),
        """
        package %s.service;%n\
        import org.springframework.stereotype.Service;%n\
        @Service%n\
        public class ItemService { %s }%n\
        """
            .formatted(basePackage, methodBody));
  }

  private static String serviceSignature(int commitIndex) {
    if (commitIndex <= 0) {
      return "public void handle() {}";
    }
    return "public void handle(long p" + commitIndex + ") {}";
  }

  private static String simpleNameFromPackage(String basePackage) {
    int dot = basePackage.lastIndexOf('.');
    String leaf = dot >= 0 ? basePackage.substring(dot + 1) : basePackage;
    if (leaf.isEmpty()) {
      return "Demo";
    }
    return Character.toUpperCase(leaf.charAt(0)) + leaf.substring(1);
  }
}
