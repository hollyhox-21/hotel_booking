package org.example.booking_service.integration.hotel;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.booking_service.integration.hotel.dto.request.ReserveInput;
import org.example.booking_service.integration.hotel.dto.response.ReservationView;
import org.example.booking_service.integration.hotel.dto.response.RoomView;
import org.example.common.ApiException;
import org.example.common.JwtSupport;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class HttpHotelClient implements HotelPort {
  private final RestClient client;
  private final String baseUrl;
  private final LoadBalancerClient discovery;
  private final JwtEncoder encoder;
  private final Retry retry;
  private final CircuitBreaker breaker;

  public HttpHotelClient(
      @Value("${app.hotel.url}") String baseUrl,
      @Value("${app.hotel.timeout-ms}") int timeout,
      @Value("${app.hotel.retry-attempts}") int attempts,
      LoadBalancerClient discovery,
      JwtEncoder encoder) {
    this.baseUrl = baseUrl;
    this.discovery = discovery;
    this.encoder = encoder;
    var http =
        java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build();
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofMillis(timeout));
    client = RestClient.builder().requestFactory(factory).build();
    retry =
        Retry.of(
            "hotel",
            RetryConfig.custom()
                .maxAttempts(attempts)
                .intervalFunction(
                    io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(100, 2))
                .retryOnException(this::retryable)
                .build());
    breaker =
        CircuitBreaker.of(
            "hotel",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(6)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .recordException(this::retryable)
                .build());
  }

  private boolean retryable(Throwable e) {
    return !(e instanceof RestClientResponseException r) || r.getStatusCode().is5xxServerError();
  }

  private String url(String path) {
    if (baseUrl.equals("http://hotel-service")) {
      var instance = discovery.choose("hotel-service");
      if (instance == null)
        throw new ApiException(503, "Hotel Service пока не зарегистрирован в Eureka");
      return instance.getUri() + path;
    }
    return baseUrl + path;
  }

  private <T> T call(java.util.function.Supplier<T> action, boolean circuit) {
    try {
      return circuit
          ? breaker.executeSupplier(() -> retry.executeSupplier(action))
          : retry.executeSupplier(action);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().is4xxClientError())
        throw new ApiException(
            e.getStatusCode().value(), "Hotel Service: номер недоступен или запрос отклонён");
      throw new ApiException(503, "Hotel Service временно недоступен");
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(503, "Hotel Service: тайм-аут или разрыв соединения");
    }
  }

  private String token() {
    return JwtSupport.token(encoder, "booking-service", "INTERNAL");
  }

  private String trace() {
    String id = MDC.get("traceId");
    return id == null ? UUID.randomUUID().toString() : id;
  }

  @Override
  public ReservationView reserve(ReserveInput input) {
    return call(
        () ->
            client
                .post()
                .uri(url("/api/internal/reservations"))
                .headers(
                    h -> {
                      h.setBearerAuth(token());
                      h.set("X-Trace-Id", trace());
                    })
                .body(input)
                .retrieve()
                .body(ReservationView.class),
        true);
  }

  @Override
  public void release(UUID id, boolean compensation) {
    call(
        () -> {
          client
              .post()
              .uri(
                  url("/api/internal/reservations/" + id + "/release?compensation=" + compensation))
              .headers(
                  h -> {
                    h.setBearerAuth(token());
                    h.set("X-Trace-Id", trace());
                  })
              .retrieve()
              .toBodilessEntity();
          return true;
        },
        false);
  }

  @Override
  public List<RoomView> recommend(LocalDate start, LocalDate end, Long hotelId) {
    // Service-to-service recommendations use the INTERNAL credential.
    return call(
        () ->
            client
                .get()
                .uri(
                    url(
                        "/api/rooms/recommend?startDate="
                            + start
                            + "&endDate="
                            + end
                            + (hotelId == null ? "" : "&hotelId=" + hotelId)))
                .headers(
                    h -> {
                      h.setBearerAuth(token());
                      h.set("X-Trace-Id", trace());
                    })
                .retrieve()
                .body(new ParameterizedTypeReference<List<RoomView>>() {}),
        true);
  }
}
