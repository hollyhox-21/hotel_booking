package org.example.booking_service.booking.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingView(
    Long id,
    Long userId,
    Long roomId,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    Instant createdAt,
    UUID requestId,
    boolean cleanupPending) {}
