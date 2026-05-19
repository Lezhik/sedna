package io.sedna.cli;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.forward.ForwardServices;
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.reverse.ReverseServices;
import io.sedna.validation.CompositeValidationEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Minimal CLI for Phase 2: forward, decode, encode, validate. */
public final class SednaCli {

  SednaCli() {}

  public static void main(String[] args) {
    int exit = new SednaCli().run(args);
    System.exit(exit);
  }

  int run(String[] args) {
    if (args.length == 0) {
      printUsage();
      return 2;
    }
    Map<String, String> options = parseOptions(args);
    String command = args[0].toLowerCase(Locale.ROOT);
    return switch (command) {
      case "forward" -> runForward(options);
      case "decode" -> runDecode(options);
      case "encode" -> runEncode(options);
      case "validate" -> runValidate(options);
      case "reverse" -> runReverse(options);
      default -> {
        System.err.println("Unknown command: " + command);
        printUsage();
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

  private static void printUsage() {
    System.err.println(
        """
        Usage:
          sedna forward --input=<file.sdna> [--output=<dir>]
          sedna decode  --input=<file.sdna>
          sedna encode  --input=<file.sdna> [--output=<file.sdna>]
          sedna validate --input=<file.sdna>
          sedna reverse  --input=<project-dir> [--output=<file.sdna>]
        """);
  }
}
