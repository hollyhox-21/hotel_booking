package org.example.booking_service.user.dto.response;

public record UserView(Long id, String username, String role, boolean enabled) {}
