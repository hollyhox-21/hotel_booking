package org.example.api_gateway.tracing;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter implements WebFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
    String id =
        incoming != null && incoming.matches("[a-zA-Z0-9-]{1,64}")
            ? incoming
            : UUID.randomUUID().toString();
    exchange.getResponse().getHeaders().set("X-Trace-Id", id);
    long started = System.nanoTime();
    return chain
        .filter(
            exchange
                .mutate()
                .request(
                    exchange.getRequest().mutate().headers(h -> h.set("X-Trace-Id", id)).build())
                .build())
        .doFinally(
            s ->
                log.atInfo()
                    .addKeyValue("traceId", id)
                    .addKeyValue("method", exchange.getRequest().getMethod())
                    .addKeyValue("path", exchange.getRequest().getPath())
                    .addKeyValue("status", exchange.getResponse().getStatusCode())
                    .addKeyValue("durationMs", (System.nanoTime() - started) / 1_000_000)
                    .log("Gateway request"));
  }
}
