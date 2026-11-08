package org.example.system;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class ApiClientTest {
  HttpServer server;
  ApiClient api;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    api = new ApiClient("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void stop() {
    if (server != null) server.stop(0);
    if (api != null) api.close();
  }

  @Test
  void sendsJsonTokenIdempotencyKeyAndCorrelation() {
    UUID key = UUID.randomUUID();
    var body = new AtomicReference<String>();
    var auth = new AtomicReference<String>();
    var id = new AtomicReference<String>();
    var trace = new AtomicReference<String>();
    server.createContext(
        "/api/bookings",
        e -> {
          body.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          auth.set(e.getRequestHeaders().getFirst("Authorization"));
          id.set(e.getRequestHeaders().getFirst("Idempotency-Key"));
          trace.set(e.getRequestHeaders().getFirst("X-Trace-Id"));
          byte[] response = "{\"id\":42}".getBytes(StandardCharsets.UTF_8);
          e.sendResponseHeaders(201, response.length);
          e.getResponseBody().write(response);
          e.close();
        });
    server.start();
    var result = api.expect(201, "POST", "/api/bookings", Map.of("roomId", 7), "jwt", key);
    assertThat(result.path("id").asLong()).isEqualTo(42);
    assertThat(body.get()).isEqualTo("{\"roomId\":7}");
    assertThat(auth.get()).isEqualTo("Bearer jwt");
    assertThat(id.get()).isEqualTo(key.toString());
    assertThat(trace.get()).startsWith("java-e2e-");
  }

  @Test
  void preservesErrorAndReportsUnexpectedStatus() {
    server.createContext(
        "/api/bookings",
        e -> {
          byte[] response = "{\"message\":\"occupied\"}".getBytes(StandardCharsets.UTF_8);
          e.sendResponseHeaders(409, response.length);
          e.getResponseBody().write(response);
          e.close();
        });
    server.start();
    assertThat(api.exchange("POST", "/api/bookings", Map.of(), null, null).status()).isEqualTo(409);
    assertThatThrownBy(() -> api.expect(201, "POST", "/api/bookings", Map.of(), null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("got 409")
        .hasMessageContaining("occupied");
  }

  @Test
  void acceptsEmptySuccessfulResponse() {
    server.createContext(
        "/api/users/1",
        e -> {
          e.sendResponseHeaders(204, -1);
          e.close();
        });
    server.start();
    assertThat(api.expect(204, "DELETE", "/api/users/1", null, "jwt", null).isNull()).isTrue();
  }
}
