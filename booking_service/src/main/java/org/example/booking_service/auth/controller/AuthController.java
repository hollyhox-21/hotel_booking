package org.example.booking_service.auth.controller;

import jakarta.validation.Valid;
import org.example.booking_service.auth.dto.request.Credentials;
import org.example.booking_service.auth.dto.response.AuthView;
import org.example.booking_service.auth.service.AuthService;
import org.example.booking_service.user.dto.response.UserView;
import org.example.booking_service.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class AuthController {
  private final AuthService auth;
  private final UserService users;

  public AuthController(AuthService auth, UserService users) {
    this.auth = auth;
    this.users = users;
  }

  @PostMapping("/auth/register")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public AuthView register(@Valid @RequestBody Credentials input) {
    return auth.register(input);
  }

  @PostMapping("/auth/login")
  public AuthView login(@Valid @RequestBody Credentials input) {
    return auth.login(input);
  }

  @GetMapping("/auth/me")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public UserView me(@AuthenticationPrincipal Jwt jwt) {
    return users.me(Long.valueOf(jwt.getSubject()));
  }
}
