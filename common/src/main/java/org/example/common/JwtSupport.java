package org.example.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtSupport {
  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    var decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("hotel-booking"));
    return decoder;
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
    return new NimbusJwtEncoder(
        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
            secret.getBytes(StandardCharsets.UTF_8)));
  }

  public static String token(JwtEncoder encoder, String subject, String role) {
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("hotel-booking")
            .subject(subject)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("roles", List.of(role))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
