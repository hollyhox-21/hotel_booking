package org.example.api_gateway.exception;

import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(-2)
public class GatewayErrors implements WebExceptionHandler {
  private final ObjectMapper mapper;

  public GatewayErrors(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public record ErrorDTO(
      Instant timestamp, int status, String message, String path, String traceId) {}

  public Mono<Void> write(ServerWebExchange exchange, int status, String message) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.valueOf(status));
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    var body =
        new ErrorDTO(
            Instant.now(),
            status,
            message,
            exchange.getRequest().getPath().value(),
            response.getHeaders().getFirst("X-Trace-Id"));
    return response.writeWith(
        Mono.just(response.bufferFactory().wrap(mapper.writeValueAsBytes(body))));
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
    if (exchange.getResponse().isCommitted()) return Mono.error(error);
    int status = error instanceof ResponseStatusException e ? e.getStatusCode().value() : 502;
    String message =
        status == 404
            ? "Маршрут не найден"
            : status == 503 ? "Сервис временно недоступен" : "Ошибка обращения к сервису";
    return write(exchange, status, message);
  }
}
