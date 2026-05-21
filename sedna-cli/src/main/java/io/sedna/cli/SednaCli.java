package io.sedna.cli;

import io.sedna.core.ExecutionProfile;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.forward.ForwardServices;
import io.sedna.persistence.CheckpointStore;
import io.sedna.persistence.FileCheckpointStore;
import io.sedna.persistence.InMemoryCheckpointStore;
import io.sedna.persistence.JdbcCheckpointStore;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.runtime.RuntimeEngine;
import io.sedna.runtime.RuntimeServices;
import io.sedna.runtime.execution.RuntimeExecutionOptions;
import io.sedna.runtime.monitor.RuntimeMonitoringServer;
import io.sedna.runtime.replay.ReplayHarness;
import io.sedna.runtime.trace.TraceHasher;
import io.sedna.training.ProjectListLoader;
import io.sedna.training.TrainingDatasetWriter;
import io.sedna.training.TrainingServices;
import io.sedna.training.model.TrainingDataset;
import io.sedna.validation.CompositeValidationEngine;
import io.sedna.validation.SemanticEquivalenceChecker;
import io.sedna.validation.SemanticGraphDiffer;
import io.sedna.validation.viz.GraphvizSemanticGraphExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.postgresql.ds.PGSimpleDataSource;

/** Minimal CLI for Phase 2: forward, decode, encode, validate. */
public final class SednaCli {

  SednaCli() {}

  /**
   * CLI entry point.
   *
   * @param args command-line arguments
   */
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
      case "replay" -> runReplay(options);
      case "diff" -> runDiff(options);
      case "visualize" -> runVisualize(options);
      case "monitor" -> runMonitor(options);
      case "train" -> runTrain(options);
      default -> {
        System.err.println("Unknown command: " + command + " (try: sedna help)");
        printUsage(System.err);
        yield 2;
      }
    };
  }

  private int runForward(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    Path output = Path.of(options.getOrDefault("output", "generated")).toAbsolutePath().normalize();
    if (input == null) {
      return 2;
    }
    try {
      if (wantsClean(options)) {
        deleteRecursively(output);
      }
      byte[] dna = Files.readAllBytes(input);
      var result =
          ForwardServices.pipeline().runToDirectory(dna, output.toAbsolutePath().normalize());
      return report(result, "Forward completed: " + output, format, "forward");
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runDecode(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var result = DnaServices.decoder().decode(dna);
      if (!result.isOk()) {
        return report(result, null, format, "decode");
      }
      SemanticGraph graph = result.value();
      if (format == CliOutput.Format.JSON) {
        CliOutput.printSuccess(
            format,
            "decode",
            "decoded",
            "\"nodes\":"
                + graph.nodes().size()
                + ",\"links\":"
                + graph.links().size()
                + ",\"registry\":\""
                + graph.vocabularyVersion().canonical()
                + "\"");
      } else {
        System.out.println(
            "nodes="
                + graph.nodes().size()
                + " links="
                + graph.links().size()
                + " registry="
                + graph.vocabularyVersion().canonical());
      }
      return 0;
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runEncode(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    Path output = options.containsKey("output") ? Path.of(options.get("output")) : input;
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null, format, "encode");
      }
      DnaEncoder encoder = DnaServices.encoder();
      byte[] encoded = encoder.encode(decoded.value()).value();
      Files.write(output, encoded);
      return report(
          Result.ok(output),
          "Wrote canonical DNA: " + output + " (" + encoded.length + " bytes)",
          format,
          "encode");
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runExecute(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try (RuntimeMonitoringServer monitor = startMonitorIfRequested(options)) {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null, format, "run");
      }
      ExecutionProfile profile = parseProfile(options.getOrDefault("profile", "DAG"));
      RuntimeExecutionOptions runtimeOptions = parseRuntimeOptions(options);
      RuntimeEngine engine = RuntimeServices.engine(resolveCheckpointStore(options));
      var trace = engine.run(decoded.value(), profile, runtimeOptions);
      if (!trace.isOk()) {
        return report(trace, null, format, "run");
      }
      if (monitor != null) {
        monitor.publishTrace(trace.value());
      }
      String hash = TraceHasher.sha256(trace.value());
      if (format == CliOutput.Format.JSON) {
        CliOutput.printSuccess(
            format,
            "run",
            "runtime completed",
            "\"profile\":\""
                + profile.name()
                + "\",\"events\":"
                + trace.value().events().size()
                + ",\"traceSha256\":\""
                + hash
                + "\"");
      } else {
        System.out.println(
            "Runtime completed: profile="
                + profile.name()
                + " steps="
                + trace.value().events().size()
                + " traceSha256="
                + hash);
        if (monitor != null) {
          System.out.println("Monitor: http://127.0.0.1:" + monitor.port() + "/trace");
        }
      }
      return 0;
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runReplay(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    if (!hasPersistentCheckpointStore(options)) {
      CliOutput.printError(
          format,
          SemanticError.global(
              io.sedna.core.ErrorCode.VALIDATION_FAILED,
              "replay requires --checkpoint-dir or --checkpoint-jdbc-url"));
      return 2;
    }
    CheckpointStore store = resolveCheckpointStore(options);
    ReplayHarness harness = RuntimeServices.replayHarness(store);
    long sequence = parseCheckpointSequence(options);
    var replayed =
        sequence > 0 ? harness.replayFromCheckpoint(sequence) : harness.replayFromLastCheckpoint();
    if (!replayed.isOk()) {
      return report(replayed, null, format, "replay");
    }
    String hash = TraceHasher.sha256(replayed.value());
    long usedSequence = sequence > 0 ? sequence : lastCheckpointSequence(store);
    CliOutput.printReplay(format, replayed.value().events().size(), hash, usedSequence);
    return 0;
  }

  private int runDiff(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path leftPath = requirePath(options, "left");
    Path rightPath = requirePath(options, "right");
    if (leftPath == null || rightPath == null) {
      return 2;
    }
    try {
      var left = DnaServices.decoder().decode(Files.readAllBytes(leftPath));
      if (!left.isOk()) {
        return report(left, null, format, "diff");
      }
      var right = DnaServices.decoder().decode(Files.readAllBytes(rightPath));
      if (!right.isOk()) {
        return report(right, null, format, "diff");
      }
      var equivalent = SemanticEquivalenceChecker.checkEquivalent(left.value(), right.value());
      boolean isEquivalent = equivalent.isOk();
      var diffs = new SemanticGraphDiffer().diff(left.value(), right.value());
      CliOutput.printDiff(format, diffs, isEquivalent);
      return isEquivalent ? 0 : 1;
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runVisualize(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    Path output =
        options.containsKey("output")
            ? Path.of(options.get("output"))
            : input.resolveSibling(input.getFileName() + ".dot");
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null, format, "visualize");
      }
      String dot = GraphvizSemanticGraphExporter.toDot(decoded.value(), safeGraphId(input));
      Files.writeString(output, dot);
      return report(Result.ok(output), "Graphviz DOT written: " + output, format, "visualize");
    } catch (IOException ex) {
      return ioError(ex, format);
    }
  }

  private int runMonitor(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    int port = Integer.parseInt(options.getOrDefault("port", "8080"));
    try (RuntimeMonitoringServer monitor = startMonitor(port)) {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null, format, "monitor");
      }
      var trace = RuntimeServices.engine().run(decoded.value());
      if (!trace.isOk()) {
        return report(trace, null, format, "monitor");
      }
      monitor.publishTrace(trace.value());
      String hash = TraceHasher.sha256(trace.value());
      if (format == CliOutput.Format.JSON) {
        CliOutput.printSuccess(
            format,
            "monitor",
            "monitoring",
            "\"port\":"
                + monitor.port()
                + ",\"traceSha256\":\""
                + hash
                + "\",\"healthUrl\":\"http://127.0.0.1:"
                + monitor.port()
                + "/health\",\"traceUrl\":\"http://127.0.0.1:"
                + monitor.port()
                + "/trace\"");
      } else {
        System.out.println("Monitor listening on http://127.0.0.1:" + monitor.port());
        System.out.println("traceSha256=" + hash);
      }
      Thread.sleep(100);
      return 0;
    } catch (IOException ex) {
      return ioError(ex, format);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return 1;
    }
  }

  private int runReverse(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    Path output =
        (options.containsKey("output")
                ? Path.of(options.get("output"))
                : input.resolveSibling(input.getFileName() + ".sdna"))
            .toAbsolutePath()
            .normalize();
    try {
      if (wantsClean(options)) {
        Files.deleteIfExists(output);
      }
    } catch (IOException ex) {
      return ioError(ex, format);
    }
    var result = ReverseServices.pipeline().reverseToFile(input, output);
    return report(result, "Reverse completed: " + output, format, "reverse");
  }

  private int runTrain(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path output = Path.of(options.getOrDefault("output", "training-out")).toAbsolutePath().normalize();
    try {
      if (wantsClean(options)) {
        deleteRecursively(output);
      }
    } catch (IOException ex) {
      return ioError(ex, format);
    }
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
        return report(loaded, null, format, "train");
      }
      trained = TrainingServices.pipeline().train(loaded.value());
    }
    if (!trained.isOk()) {
      return report(trained, null, format, "train");
    }
    var written = new TrainingDatasetWriter().write(trained.value(), output.toAbsolutePath().normalize());
    if (!written.isOk()) {
      return report(written, null, format, "train");
    }
    var artifacts = written.value();
    if (format == CliOutput.Format.JSON) {
      CliOutput.printSuccess(
          format,
          "train",
          "training completed",
          "\"projects\":"
              + trained.value().projects().size()
              + ",\"fingerprint\":\""
              + trained.value().datasetFingerprint()
              + "\",\"manifest\":\""
              + artifacts.manifestPath()
              + "\"");
    } else {
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
    }
    return 0;
  }

  private int runValidate(Map<String, String> options) {
    CliOutput.Format format = outputFormat(options);
    Path input = requirePath(options, "input");
    if (input == null) {
      return 2;
    }
    try {
      byte[] dna = Files.readAllBytes(input);
      var decoded = DnaServices.decoder().decode(dna);
      if (!decoded.isOk()) {
        return report(decoded, null, format, "validate");
      }
      var registry = InMemorySemanticRegistry.bootstrap();
      var engine = CompositeValidationEngine.standard(registry);
      var validated = engine.validate(decoded.value());
      if (!validated.isOk()) {
        return report(validated, null, format, "validate");
      }
      if (!validated.value().valid()) {
        SemanticError error = validated.value().errors().getFirst();
        return report(Result.err(error), null, format, "validate");
      }
      return report(Result.ok(Boolean.TRUE), "Validation OK", format, "validate");
    } catch (IOException ex) {
      return ioError(ex, format);
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

  private static String safeGraphId(Path input) {
    var name = input.getFileName();
    return name != null ? name.toString() : "sedna_graph";
  }

  private static boolean hasPersistentCheckpointStore(Map<String, String> options) {
    String dir = options.get("checkpoint-dir");
    String jdbc = options.get("checkpoint-jdbc-url");
    return (dir != null && !dir.isBlank()) || (jdbc != null && !jdbc.isBlank());
  }

  private static CheckpointStore resolveCheckpointStore(Map<String, String> options) {
    String checkpointDir = options.get("checkpoint-dir");
    if (checkpointDir != null && !checkpointDir.isBlank()) {
      return new FileCheckpointStore(Path.of(checkpointDir));
    }
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

  private static long parseCheckpointSequence(Map<String, String> options) {
    String value = options.get("checkpoint-sequence");
    if (value == null || value.isBlank()) {
      return 0L;
    }
    return Long.parseLong(value);
  }

  private static long lastCheckpointSequence(CheckpointStore store) {
    var listed = store.listOrdered();
    if (!listed.isOk() || listed.value().isEmpty()) {
      return 0L;
    }
    return listed.value().getLast().sequenceNumber();
  }

  private static RuntimeMonitoringServer startMonitor(int port) {
    var started = RuntimeMonitoringServer.start(port);
    if (!started.isOk()) {
      throw new IllegalStateException(started.error().message());
    }
    return started.value();
  }

  private static RuntimeMonitoringServer startMonitorIfRequested(Map<String, String> options) {
    String portValue = options.get("monitor-port");
    if (portValue == null || portValue.isBlank()) {
      return null;
    }
    return startMonitor(Integer.parseInt(portValue));
  }

  private static CliOutput.Format outputFormat(Map<String, String> options) {
    return CliOutput.Format.parse(options.get("format"));
  }

  private static int ioError(IOException ex, CliOutput.Format format) {
    if (format == CliOutput.Format.JSON) {
      System.out.println(
          "{\"status\":\"error\",\"code\":\"INTERNAL\",\"nodeId\":0,\"message\":\""
              + ex.getMessage().replace("\"", "\\\"")
              + "\"}");
    } else {
      System.err.println("I/O error: " + ex.getMessage());
    }
    return 1;
  }

  private static int report(
      Result<?, SemanticError> result, String successMessage, CliOutput.Format format, String command) {
    if (result.isOk()) {
      if (successMessage != null) {
        CliOutput.printSuccess(format, command, successMessage, null);
      }
      return 0;
    }
    CliOutput.printError(format, result.error());
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
        } else if (arg.length() > 2) {
          options.put(arg.substring(2), "true");
        }
      }
    }
    return options;
  }

  private static boolean wantsClean(Map<String, String> options) {
    return "true".equalsIgnoreCase(options.get("clean"));
  }

  private static void deleteRecursively(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException ex) {
                  throw new java.io.UncheckedIOException(ex);
                }
              });
    } catch (java.io.UncheckedIOException ex) {
      throw ex.getCause();
    }
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
          sedna forward --input=<file.sdna> [--output=<dir>] [--clean]
          sedna decode  --input=<file.sdna>
          sedna encode  --input=<file.sdna> [--output=<file.sdna>]
          sedna validate --input=<file.sdna>
          sedna reverse  --input=<project-dir> [--output=<file.sdna>] [--clean]
          sedna run      --input=<file.sdna> [--profile=DAG|STATEFUL|SUPERVISOR] [--checkpoint-dir=<dir>|--checkpoint-jdbc-url=<jdbc>] [--monitor-port=<port>] [--format=json]
          sedna replay   [--checkpoint-sequence=<n>] [--checkpoint-dir=<dir>|--checkpoint-jdbc-url=<jdbc>] [--format=json]
          sedna diff     --left=<a.sdna> --right=<b.sdna> [--format=json]
          sedna visualize --input=<file.sdna> [--output=<file.dot>]
          sedna monitor  --input=<file.sdna> [--port=8080] [--format=json]
          sedna train    --projects=<list.txt> | --corpus=<repo-root> [--output=<dir>] [--clean] [--format=json]

        Global flags: --help, --format=json, --clean (forward/reverse/train: remove output before write)
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
              "Execute runtime and optionally expose /trace monitoring endpoint.";
          case "replay" -> "Replay execution from persisted checkpoint store.";
          case "diff" -> "Semantic diff between two DNA files.";
          case "visualize" -> "Export semantic graph as Graphviz DOT.";
          case "monitor" -> "Run once and expose trace on HTTP /trace endpoint.";
          case "train" ->
              "Build training dataset (manifest + checksum + reproducibility report) from project list or repo corpus.";
          default -> "See `sedna help` for supported commands.";
        };
    out.println(command + ": " + detail);
  }
}
