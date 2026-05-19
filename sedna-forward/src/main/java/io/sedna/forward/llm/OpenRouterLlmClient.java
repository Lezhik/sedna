package io.sedna.forward.llm;

import io.sedna.core.GenomeNode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** OpenRouter HTTP client for method bodies only. Disabled when API key absent. */
public final class OpenRouterLlmClient implements LlmClient {

  private final HttpClient httpClient;
  private final String baseUrl;
  private final String apiKey;
  private final String model;

  public OpenRouterLlmClient() {
    this(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        env("SEDNA_LLM_BASE_URL", "https://openrouter.ai/api/v1"),
        env("OPENROUTER_API_KEY", ""),
        env("SEDNA_LLM_MODEL", "openai/gpt-4o-mini"));
  }

  OpenRouterLlmClient(HttpClient httpClient, String baseUrl, String apiKey, String model) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.apiKey = apiKey;
    this.model = model;
  }

  public static LlmClient createOrDisabled() {
    OpenRouterLlmClient client = new OpenRouterLlmClient();
    if (client.apiKey.isBlank()) {
      return DisabledLlmClient.INSTANCE;
    }
    return client;
  }

  @Override
  public Result<String, SemanticError> generateMethodBody(GenomeNode node, String methodSignature) {
    if (apiKey.isBlank()) {
      return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
    }
    try {
      String prompt =
          "Generate only a Java method body (no signature, no class) for: " + methodSignature;
      String json =
          String.format(
              "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}]}",
              model, quoteJson(prompt));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/chat/completions"))
              .timeout(Duration.ofSeconds(30))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
      }
      if (json.length() > 65_536) {
        return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
      }
      String body = LlmResponseSanitizer.sanitize(extractContent(response.body()));
      if (body.isBlank()) {
        return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
      }
      return Result.ok(body);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
    } catch (java.io.IOException ex) {
      return DisabledLlmClient.INSTANCE.generateMethodBody(node, methodSignature);
    }
  }

  private static String extractContent(String responseBody) {
    int idx = responseBody.indexOf("\"content\"");
    if (idx < 0) {
      return "";
    }
    int start = responseBody.indexOf('"', idx + 10);
    if (start < 0) {
      return "";
    }
    int end = responseBody.indexOf('"', start + 1);
    if (end < 0) {
      return "";
    }
    return responseBody.substring(start + 1, end);
  }

  private static String quoteJson(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }

  private static String env(String key, String defaultValue) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
