package org.example.booking_service.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserInput(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,50}") String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    @Pattern(regexp = "USER|ADMIN") @NotNull String role) {}
