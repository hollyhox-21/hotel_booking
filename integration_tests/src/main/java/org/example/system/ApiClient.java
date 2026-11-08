package org.example.system;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** HTTP adapter shared by Java end-to-end fixtures and demo data. */
public final class ApiClient implements AutoCloseable {
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final ObjectMapper json = new ObjectMapper();
  private final String baseUrl;

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public static String gatewayUrl() {
    return System.getenv().getOrDefault("GATEWAY_URL", "http://localhost:8080");
  }

  public record Reply(int status, JsonNode body) {}

  public Reply exchange(String method, String path, Object body, String token, UUID key) {
    var request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(25))
            .header("X-Trace-Id", "java-e2e-" + UUID.randomUUID());
    if (token != null) request.header("Authorization", "Bearer " + token);
    if (key != null) request.header("Idempotency-Key", key.toString());
    if (body != null) request.header("Content-Type", "application/json");
    request.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    try {
      var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return new Reply(
          response.statusCode(),
          response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request interrupted", e);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot reach " + baseUrl + path, e);
    }
  }

  public JsonNode expect(
      int expected, String method, String path, Object body, String token, UUID key) {
    var reply = exchange(method, path, body, token, key);
    if (reply.status() != expected)
      throw new IllegalStateException(
          method
              + " "
              + path
              + ": expected "
              + expected
              + ", got "
              + reply.status()
              + ": "
              + reply.body());
    return reply.body();
  }

  public JsonNode get(String path, String token) {
    return expect(200, "GET", path, null, token, null);
  }

  public String login(String username, String password) {
    return expect(
            200,
            "POST",
            "/api/auth/login",
            Map.of("username", username, "password", password),
            null,
            null)
        .path("token")
        .asText();
  }

  @Override
  public void close() {
    http.close();
  }
}
