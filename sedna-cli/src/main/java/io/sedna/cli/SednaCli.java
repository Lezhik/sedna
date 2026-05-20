package io.sedna.cli;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.forward.ForwardServices;
import io.sedna.persistence.CheckpointStore;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.persistence.JdbcCheckpointStore;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.runtime.RuntimeEngine;
import io.sedna.runtime.RuntimeServices;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.trace.TraceHasher;
import io.sedna.training.ProjectListLoader;
import io.sedna.training.TrainingDatasetWriter;
import io.sedna.training.TrainingServices;
import io.sedna.training.model.TrainingDataset;
import io.sedna.validation.CompositeValidationEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.postgresql.ds.PGSimpleDataSource;

/** Minimal CLI for Phase 2: forward, decode, encode, validate. */
public final class SednaCli {

  SednaCli() {}

  public static void main(String[] args) {
    int exit = new SednaCli().run(args);
    System.exit(exit);
  }

  int run(String[] args) {
    if (args.length == 0 || isHelp(args[0])) {
      printUsage(System.out);
      return args.length == 0 ? 2 : 0;
    }
    Map<String, String> options = parseOptions(args);
    if (options.containsKey("help") || options.containsKey("h")) {
      printCommandHelp(args[0].toLowerCase(Locale.ROOT), System.out);
      return 0;
    }
    String command = args[0].toLowerCase(Locale.ROOT);
    return switch (command) {
      case "help" -> {
        printUsage(System.out);
        yield 0;
      }
      case "forward" -> runForward(options);
      case "decode" -> runDecode(options);
      case "encode" -> runEncode(options);
      case "validate" -> runValidate(options);
      case "reverse" -> runReverse(options);
      case "run" -> runExecute(options);
      case "train" -> runTrain(options);
      default -> {
        System.err.println("Unknown command: " + command + " (try: sedna help)");
        printUsage(System.err);
        yield 2;
      }
    };
  }

  private int runForward(Map<String, String> options) {
    Path input = requirePath(options, "input");
    Path output = Path.of(options.getOrDefault("output", "generated"));
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var result =
          ForwardServices.pipeline().runToDirectory(dna, output.toAbsolutePath().normalize());
      return report(result, "Forward completed: " + output);
    } catch (IOException ex) {
      System.err.println("I/O error: " + ex.getMessage());
      return 1;
    }
  }

  private int runDecode(Map<String, String> options) {
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var result = DnaServices.decoder().decode(dna);
      if (!result.isOk()) {
        return report(result, null);
      }
      SemanticGraph graph = result.value();
      System.out.println(
          "nodes="
              + graph.nodes().size()
              + " links="
              + graph.links().size()
              + " registry="
              + graph.vocabularyVersion().canonical());
      return 0;
    } catch (IOException ex) {
      System.err.println("I/O error: " + ex.getMessage());
      return 1;
    }
  }

  private int runEncode(Map<String, String> options) {
    Path input = requirePath(options, "input");
    Path output = options.containsKey("output") ? Path.of(options.get("output")) : input;
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null);
      }
      DnaEncoder encoder = DnaServices.encoder();
      byte[] encoded = encoder.encode(decoded.value()).value();
      Files.write(output, encoded);
      System.out.println("Wrote canonical DNA: " + output + " (" + encoded.length + " bytes)");
      return 0;
    } catch (IOException ex) {
      System.err.println("I/O error: " + ex.getMessage());
      return 1;
    }
  }

  private int runExecute(Map<String, String> options) {
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null);
      }
      ExecutionProfile profile = parseProfile(options.getOrDefault("profile", "DAG"));
      RuntimeExecutionOptions runtimeOptions = parseRuntimeOptions(options);
      RuntimeEngine engine = RuntimeServices.engine(resolveCheckpointStore(options));
      var trace = engine.run(decoded.value(), profile, runtimeOptions);
      if (!trace.isOk()) {
        return report(trace, null);
      }
      String hash = TraceHasher.sha256(trace.value());
      System.out.println(
          "Runtime completed: profile="
              + profile.name()
              + " steps="
              + trace.value().events().size()
              + " traceSha256="
              + hash);
      return 0;
    } catch (IOException ex) {
      System.err.println("I/O error: " + ex.getMessage());
      return 1;
    }
  }

  private int runReverse(Map<String, String> options) {
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    Path output =
        options.containsKey("output")
            ? Path.of(options.get("output"))
            : input.resolveSibling(input.getFileName() + ".sdna");
    var result =
        ReverseServices.pipeline().reverseToFile(input, output.toAbsolutePath().normalize());
    return report(result, "Reverse completed: " + output);
  }

  private int runTrain(Map<String, String> options) {
    Path output = Path.of(options.getOrDefault("output", "training-out"));
    Result<TrainingDataset, SemanticError> trained;
    if (options.containsKey("corpus")) {
      Path repoRoot = Path.of(options.get("corpus")).toAbsolutePath().normalize();
      trained = TrainingServices.pipeline().trainCorpus(repoRoot);
    } else {
      Path projects = requirePath(options, "projects");
      if (projects == null) {
        return 2;
      }
      var loaded = new ProjectListLoader().load(projects);
      if (!loaded.isOk()) {
        return report(loaded, null);
      }
      trained = TrainingServices.pipeline().train(loaded.value());
    }
    if (!trained.isOk()) {
      return report(trained, null);
    }
    var written = new TrainingDatasetWriter().write(trained.value(), output.toAbsolutePath().normalize());
    if (!written.isOk()) {
      return report(written, null);
    }
    var artifacts = written.value();
    System.out.println(
        "Training completed: projects="
            + trained.value().projects().size()
            + " fingerprint="
            + trained.value().datasetFingerprint()
            + " manifest="
            + artifacts.manifestPath()
            + " checksum="
            + artifacts.manifestChecksumPath()
            + " report="
            + artifacts.reproducibilityReportPath());
    return 0;
  }

  private int runValidate(Map<String, String> options) {
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null);
      }
      var registry = InMemorySemanticRegistry.bootstrap();
      var engine = CompositeValidationEngine.standard(registry);
      var validated = engine.validate(decoded.value());
      if (!validated.isOk()) {
        return report(validated, null);
      }
      if (!validated.value().valid()) {
        SemanticError error = validated.value().errors().getFirst();
        return report(Result.err(error), null);
      }
      System.out.println("Validation OK");
      return 0;
    } catch (IOException ex) {
      System.err.println("I/O error: " + ex.getMessage());
      return 1;
    }
  }

  private static ExecutionProfile parseProfile(String value) {
    return ExecutionProfile.valueOf(value.toUpperCase(Locale.ROOT));
  }

  private static RuntimeExecutionOptions parseRuntimeOptions(Map<String, String> options) {
    String failureNode = options.get("inject-failure-node-id");
    if (failureNode == null || failureNode.isBlank()) {
      return RuntimeExecutionOptions.DEFAULT;
    }
    return RuntimeExecutionOptions.injectFailure(Long.parseLong(failureNode));
  }

  private static CheckpointStore resolveCheckpointStore(Map<String, String> options) {
    String jdbcUrl = options.get("checkpoint-jdbc-url");
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return new InMemoryCheckpointStore();
    }
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(jdbcUrl);
    String user = options.get("checkpoint-db-user");
    String password = options.get("checkpoint-db-password");
    if (user != null && !user.isBlank()) {
      dataSource.setUser(user);
    }
    if (password != null) {
      dataSource.setPassword(password);
    }
    JdbcCheckpointStore store = new JdbcCheckpointStore(dataSource);
    try {
      store.migrate();
    } catch (java.sql.SQLException ex) {
      throw new IllegalStateException("Checkpoint schema migration failed: " + ex.getMessage(), ex);
    }
    return store;
  }

  private static int report(Result<?, SemanticError> result, String successMessage) {
    if (result.isOk()) {
      if (successMessage != null) {
        System.out.println(successMessage);
      }
      return 0;
    }
    SemanticError error = result.error();
    System.err.println(error.code() + " [nodeId=" + error.nodeId() + "]: " + error.message());
    return 1;
  }

  private static Path requirePath(Map<String, String> options, String key) {
    String value = options.get(key);
    if (value == null || value.isBlank()) {
      System.err.println("Missing --" + key + "=<path>");
      return null;
    }
    return Path.of(value);
  }

  private static Map<String, String> parseOptions(String[] args) {
    Map<String, String> options = new HashMap<>();
    for (int i = 1; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--")) {
        int eq = arg.indexOf('=');
        if (eq > 2) {
          options.put(arg.substring(2, eq), arg.substring(eq + 1));
        }
      }
    }
    return options;
  }

  private static boolean isHelp(String arg) {
    String normalized = arg.toLowerCase(Locale.ROOT);
    return normalized.equals("--help") || normalized.equals("-h") || normalized.equals("help");
  }

  private static void printUsage(java.io.PrintStream out) {
    out.println(
        """
        SEDNA CLI — semantic DNA toolkit

        Usage:
          sedna help
          sedna forward --input=<file.sdna> [--output=<dir>]
          sedna decode  --input=<file.sdna>
          sedna encode  --input=<file.sdna> [--output=<file.sdna>]
          sedna validate --input=<file.sdna>
          sedna reverse  --input=<project-dir> [--output=<file.sdna>]
          sedna run      --input=<file.sdna> [--profile=DAG|STATEFUL|SUPERVISOR] [--checkpoint-jdbc-url=<jdbc>]
          sedna train    --projects=<list.txt> | --corpus=<repo-root> [--output=<dir>]

        Global flags: --help
        Errors print: <ErrorCode> [nodeId=…]: message
        """);
  }

  private static void printCommandHelp(String command, java.io.PrintStream out) {
    String detail =
        switch (command) {
          case "forward" -> "Generate Spring Boot project tree from DNA (LLM may fill method bodies).";
          case "decode" -> "Decode DNA and print graph summary.";
          case "encode" -> "Re-encode DNA to canonical bytes.";
          case "validate" -> "Validate DNA graph against registry and rules.";
          case "reverse" -> "Reverse-engineer DNA from a project folder.";
          case "run" ->
              "Execute runtime (DAG|STATEFUL|SUPERVISOR) and print trace SHA-256 (optional PostgreSQL checkpoints).";
          case "train" ->
              "Build training dataset (manifest + checksum + reproducibility report) from project list or repo corpus.";
          default -> "See `sedna help` for supported commands.";
        };
    out.println(command + ": " + detail);
  }
}
