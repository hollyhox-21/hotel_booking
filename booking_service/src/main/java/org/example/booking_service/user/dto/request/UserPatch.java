package org.example.booking_service.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPatch(
    @Pattern(regexp = "[a-zA-Z0-9_.-]{3,50}") String username,
    @Size(min = 8, max = 72) String password,
    @Pattern(regexp = "USER|ADMIN") String role,
    Boolean enabled) {}
