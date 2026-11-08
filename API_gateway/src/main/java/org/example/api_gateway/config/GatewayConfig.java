package org.example.api_gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.example.api_gateway.exception.GatewayErrors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewayConfig {
  private final GatewayErrors errors;

  public GatewayConfig(GatewayErrors errors) {
    this.errors = errors;
  }

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder
        .routes()
        .route(
            "booking",
            r ->
                r.path("/api/bookings/**", "/api/users/**", "/api/auth/**")
                    .uri("lb://booking-service"))
        .route("hotel", r -> r.path("/api/hotels/**", "/api/rooms/**").uri("lb://hotel-service"))
        .build();
  }

  @Bean
  public ReactiveJwtDecoder decoder(@Value("${app.jwt.secret}") String secret) {
    var d =
        NimbusReactiveJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer("hotel-booking"));
    return d;
  }

  @Bean
  public SecurityWebFilterChain security(ServerHttpSecurity http) {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthorityPrefix("ROLE_");
    roles.setAuthoritiesClaimName("roles");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return http.csrf(c -> c.disable())
        .httpBasic(c -> c.disable())
        .formLogin(c -> c.disable())
        .authorizeExchange(
            a ->
                a.pathMatchers(
                        "/api/internal/**",
                        "/api/rooms/*/release",
                        "/api/rooms/*/confirm-availability")
                    .denyAll()
                    .pathMatchers("/api/auth/register", "/api/auth/login", "/actuator/health")
                    .permitAll()
                    .pathMatchers("/api/bookings/**")
                    .hasRole("USER")
                    .pathMatchers("/api/users/**", "/api/rooms/all", "/api/hotels/statistics")
                    .hasRole("ADMIN")
                    .pathMatchers(HttpMethod.GET, "/api/hotels/**", "/api/rooms/**", "/api/auth/me")
                    .hasAnyRole("USER", "ADMIN")
                    .pathMatchers("/api/hotels/**", "/api/rooms/**")
                    .hasRole("ADMIN")
                    .anyExchange()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((exchange, error) -> error(exchange, 401))
                    .accessDeniedHandler((exchange, error) -> error(exchange, 403)))
        .oauth2ResourceServer(
            o ->
                o.jwt(
                        j ->
                            j.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(converter)))
                    .authenticationEntryPoint((exchange, error) -> error(exchange, 401))
                    .accessDeniedHandler((exchange, error) -> error(exchange, 403)))
        .build();
  }

  private Mono<Void> error(org.springframework.web.server.ServerWebExchange exchange, int status) {
    return errors.write(
        exchange, status, status == 401 ? "Требуется действующий JWT" : "Доступ запрещён");
  }
}
