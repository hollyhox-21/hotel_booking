package org.example.common;

import java.time.Instant;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  @org.springframework.core.annotation.Order(0)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      name = "spring.h2.console.enabled",
      havingValue = "true")
  SecurityFilterChain h2ConsoleSecurity(HttpSecurity http) throws Exception {
    return http.securityMatcher("/h2-console/**")
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    org.springframework.security.web.AuthenticationEntryPoint unauthorized =
        (req, res, ex) -> {
          res.setStatus(401);
          res.setContentType("application/json;charset=UTF-8");
          res.getWriter()
              .write(
                  mapper.writeValueAsString(
                      new ErrorDTO(
                          Instant.now(),
                          401,
                          "Требуется действующий JWT",
                          req.getRequestURI(),
                          res.getHeader("X-Trace-Id"))));
        };
    org.springframework.security.web.access.AccessDeniedHandler forbidden =
        (req, res, ex) -> {
          res.setStatus(403);
          res.setContentType("application/json;charset=UTF-8");
          res.getWriter()
              .write(
                  mapper.writeValueAsString(
                      new ErrorDTO(
                          Instant.now(),
                          403,
                          "Доступ запрещён",
                          req.getRequestURI(),
                          res.getHeader("X-Trace-Id"))));
        };
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/**",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/internal/**",
                        "/api/rooms/*/release",
                        "/api/rooms/*/confirm-availability")
                    .hasRole("INTERNAL")
                    .requestMatchers("/api/bookings/**")
                    .hasRole("USER")
                    .requestMatchers("/api/users/**", "/api/rooms/all", "/api/hotels/statistics")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/rooms/recommend")
                    .hasAnyRole("USER", "INTERNAL")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/hotels/**", "/api/rooms/**")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/api/hotels/**", "/api/rooms/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(forbidden))
        .build();
  }
}
