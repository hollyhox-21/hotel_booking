package org.example.booking_service.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.example.booking_service.shared.dto.response.PageView;
import org.example.booking_service.user.dto.request.UserInput;
import org.example.booking_service.user.dto.request.UserPatch;
import org.example.booking_service.user.dto.response.UserView;
import org.example.booking_service.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class UserController {
  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
  }

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public PageView<UserView> users(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return users.list(page, size);
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public UserView user(@PathVariable Long id) {
    return users.get(id);
  }

  @PostMapping("/users")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public UserView create(@Valid @RequestBody UserInput input) {
    return users.create(input);
  }

  @PatchMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public UserView update(
      @PathVariable Long id,
      @Valid @RequestBody UserPatch input,
      @AuthenticationPrincipal Jwt jwt) {
    return users.update(id, input, Long.valueOf(jwt.getSubject()));
  }

  @DeleteMapping("/users/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    users.delete(id, Long.valueOf(jwt.getSubject()));
  }
}
