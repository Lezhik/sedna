package io.sedna.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerCommitTrajectoryTest {

  @Test
  void trainingBuildsMultiSnapshotTrajectory(@TempDir Path temp) throws IOException, GitAPIException {
    Path project = temp.resolve("demo-project");
    Files.createDirectories(project.resolve("src/main/java/com/acme/demo/domain"));
    Files.writeString(
        project.resolve("src/main/java/com/acme/demo/DemoApplication.java"),
        """
        package com.acme.demo;
        import org.springframework.boot.autoconfigure.SpringBootApplication;
        @SpringBootApplication
        public class DemoApplication {}
        """);
    Files.writeString(
        project.resolve("src/main/java/com/acme/demo/domain/Item.java"),
        "package com.acme.demo.domain;\npublic class Item { private long id; }\n");
    Files.createDirectories(project.resolve("src/main/java/com/acme/demo/service"));
    Files.writeString(
        project.resolve("src/main/java/com/acme/demo/service/ItemService.java"),
        """
        package com.acme.demo.service;
        import org.springframework.stereotype.Service;
        @Service
        public class ItemService { public void handle() {} }
        """);
    Files.createDirectories(project.resolve("src/main/java/com/acme/demo/web"));
    Files.writeString(
        project.resolve("src/main/java/com/acme/demo/web/ItemController.java"),
        """
        package com.acme.demo.web;
        import com.acme.demo.service.ItemService;
        import org.springframework.web.bind.annotation.RestController;
        @RestController
        public class ItemController {
          private final ItemService itemService;
          public ItemController(ItemService itemService) { this.itemService = itemService; }
          public void handle() { itemService.handle(); }
        }
        """);
    Files.writeString(project.resolve("build.gradle.kts"), "plugins { java }\n");

    try (Git git = Git.init().setDirectory(project.toFile()).call()) {
      git.add().addFilepattern(".").call();
      git.commit().setMessage("initial").call();
      Files.writeString(
          project.resolve("src/main/java/com/acme/demo/service/ItemService.java"),
          """
          package com.acme.demo.service;
          import org.springframework.stereotype.Service;
          @Service
          public class ItemService { public void handle(long id) {} }
          """);
      git.add().addFilepattern(".").call();
      git.commit().setMessage("change service signature").call();
    }

    var trained = TrainingServices.pipeline().trainProject(project);
    assertTrue(trained.isOk(), () -> String.valueOf(trained.error()));
    assertEquals(2, trained.value().trajectory().snapshots().size());
    assertTrue(trained.value().trajectory().deltas().size() >= 1);
  }
}
