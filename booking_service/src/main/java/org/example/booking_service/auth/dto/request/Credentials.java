package org.example.booking_service.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Credentials(
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_.-]{3,50}") String username,
    @NotBlank @Size(min = 8, max = 72) String password) {}
