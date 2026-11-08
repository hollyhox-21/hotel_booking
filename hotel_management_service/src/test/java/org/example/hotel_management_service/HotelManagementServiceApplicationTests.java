package org.example.hotel_management_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HotelManagementServiceApplicationTests {

  @Test
  public void contextLoads() {}

  @org.springframework.boot.test.web.server.LocalServerPort int port;

  @org.junit.jupiter.api.Test
  public void h2ConsoleIsAvailableLocallyAndApiStillRequiresJwt() throws Exception {
    var client = java.net.http.HttpClient.newHttpClient();
    var console =
        client.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://localhost:" + port + "/h2-console/"))
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    org.assertj.core.api.Assertions.assertThat(console.statusCode()).isEqualTo(200);
    org.assertj.core.api.Assertions.assertThat(console.body()).contains("login.jsp");
    org.assertj.core.api.Assertions.assertThat(console.headers().firstValue("X-Frame-Options"))
        .contains("SAMEORIGIN");
    var api =
        client.send(
            java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://localhost:" + port + "/api/hotels"))
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    org.assertj.core.api.Assertions.assertThat(api.statusCode()).isEqualTo(401);
    org.assertj.core.api.Assertions.assertThat(api.headers().firstValue("X-Frame-Options"))
        .contains("DENY");
  }
}
