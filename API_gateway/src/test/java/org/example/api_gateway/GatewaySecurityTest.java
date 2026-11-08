package org.example.api_gateway;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "eureka.client.enabled=false")
public class GatewaySecurityTest {
  @LocalServerPort int port;

  public WebTestClient client() {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  public String token(String role) {
    var encoder =
        new NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
                "demo-only-change-me-0123456789abcdef0123456789abcdef"
                    .getBytes(StandardCharsets.UTF_8)));
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                    .issuer("hotel-booking")
                    .subject("1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("roles", List.of(role))
                    .build()))
        .getTokenValue();
  }

  @Test
  public void unauthorizedIsStructuredAndCorrelated() {
    client()
        .get()
        .uri("/api/bookings")
        .header("X-Trace-Id", "test-trace")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("X-Trace-Id", "test-trace")
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.traceId")
        .isEqualTo("test-trace");
  }

  @Test
  public void adminCannotBookAndUserCannotAdminister() {
    client()
        .get()
        .uri("/api/bookings")
        .headers(h -> h.setBearerAuth(token("ADMIN")))
        .exchange()
        .expectStatus()
        .isForbidden();
    client()
        .get()
        .uri("/api/users")
        .headers(h -> h.setBearerAuth(token("USER")))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  public void internalRoutesAreNeverPublished() {
    for (String path :
        List.of(
            "/api/internal/reservations",
            "/api/rooms/1/release",
            "/api/rooms/1/confirm-availability"))
      client()
          .post()
          .uri(path)
          .headers(h -> h.setBearerAuth(token("INTERNAL")))
          .exchange()
          .expectStatus()
          .isForbidden();
  }
}
