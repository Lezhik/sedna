package io.sedna.runtime.monitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.runtime.trace.ExecutionTrace;
import io.sedna.runtime.trace.TraceHasher;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Minimal HTTP monitoring endpoint for runtime traces (Phase 14). */
public final class RuntimeMonitoringServer implements AutoCloseable {

  private final HttpServer server;
  private final AtomicReference<ExecutionTrace> lastTrace = new AtomicReference<>();

  private RuntimeMonitoringServer(HttpServer server) {
    this.server = server;
  }

  public static Result<RuntimeMonitoringServer, SemanticError> start(int port) {
    try {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
      RuntimeMonitoringServer monitoring = new RuntimeMonitoringServer(httpServer);
      httpServer.createContext("/health", monitoring::handleHealth);
      httpServer.createContext("/trace", monitoring::handleTrace);
      httpServer.setExecutor(null);
      httpServer.start();
      return Result.ok(monitoring);
    } catch (IOException ex) {
      return Result.err(
          io.sedna.core.SemanticError.global(io.sedna.core.ErrorCode.INTERNAL, ex.getMessage()));
    }
  }

  public void publishTrace(ExecutionTrace trace) {
    lastTrace.set(trace);
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    write(exchange, 200, "{\"status\":\"UP\"}\n");
  }

  private void handleTrace(HttpExchange exchange) throws IOException {
    ExecutionTrace trace = lastTrace.get();
    if (trace == null) {
      write(exchange, 404, "{\"status\":\"NO_TRACE\"}\n");
      return;
    }
    String hash = TraceHasher.sha256(trace);
    String body =
        "{\"status\":\"OK\",\"events\":"
            + trace.events().size()
            + ",\"traceSha256\":\""
            + hash
            + "\"}\n";
    write(exchange, 200, body);
  }

  private static void write(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream stream = exchange.getResponseBody()) {
      stream.write(bytes);
    }
  }
}
