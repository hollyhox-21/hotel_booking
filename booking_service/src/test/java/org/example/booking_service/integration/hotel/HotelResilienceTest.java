package org.example.booking_service.integration.hotel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.booking_service.integration.hotel.dto.request.ReserveInput;
import org.example.common.ApiException;
import org.junit.jupiter.api.*;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public class HotelResilienceTest {
  HttpServer server;
  ExecutorService executor;
  HttpHotelClient client;
  AtomicInteger calls;

  public void start(int timeout) throws Exception {
    calls = new AtomicInteger();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    executor = Executors.newCachedThreadPool();
    server.setExecutor(executor);
    JwtEncoder encoder = mock(JwtEncoder.class);
    when(encoder.encode(any(JwtEncoderParameters.class)))
        .thenReturn(Jwt.withTokenValue("token").header("alg", "HS256").subject("service").build());
    client =
        new HttpHotelClient(
            "http://127.0.0.1:" + server.getAddress().getPort(),
            timeout,
            3,
            mock(LoadBalancerClient.class),
            encoder);
  }

  public ReserveInput input() {
    return new ReserveInput(
        UUID.randomUUID(),
        1L,
        1L,
        null,
        false,
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(2),
        null);
  }

  @AfterEach
  public void stop() {
    if (server != null) server.stop(0);
    if (executor != null) executor.shutdownNow();
  }

  @Test
  public void retriesTransientFailuresWithSameRequestAndOpensCircuit() throws Exception {
    start(200);
    Set<String> keys = ConcurrentHashMap.newKeySet();
    server.createContext(
        "/",
        exchange -> {
          calls.incrementAndGet();
          keys.add(new String(exchange.getRequestBody().readAllBytes()));
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });
    server.start();
    ReserveInput input = input();
    for (int i = 0; i < 4; i++)
      assertThatThrownBy(() -> client.reserve(input)).isInstanceOf(ApiException.class);
    assertThat(calls.get()).isEqualTo(12);
    assertThat(keys).hasSize(1);
    assertThatThrownBy(() -> client.reserve(input)).isInstanceOf(ApiException.class);
    assertThat(calls.get()).isEqualTo(12);
  }

  @Test
  public void timeoutIsBoundedAndRetried() throws Exception {
    start(80);
    server.createContext(
        "/",
        exchange -> {
          calls.incrementAndGet();
          try {
            Thread.sleep(600);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });
    server.start();
    long before = System.nanoTime();
    assertThatThrownBy(() -> client.reserve(input())).isInstanceOf(ApiException.class);
    assertThat(Duration.ofNanos(System.nanoTime() - before)).isLessThan(Duration.ofSeconds(3));
    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  public void businessConflictIsNotRetried() throws Exception {
    start(300);
    server.createContext(
        "/",
        exchange -> {
          calls.incrementAndGet();
          exchange.sendResponseHeaders(409, -1);
          exchange.close();
        });
    server.start();
    assertThatThrownBy(() -> client.reserve(input())).isInstanceOf(ApiException.class);
    assertThat(calls.get()).isEqualTo(1);
  }
}
