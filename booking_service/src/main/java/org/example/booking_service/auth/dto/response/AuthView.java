package org.example.booking_service.auth.dto.response;

import org.example.booking_service.user.dto.response.UserView;

public record AuthView(String token, String tokenType, long expiresIn, UserView user) {}
