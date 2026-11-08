package org.example.booking_service.integration.hotel.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationView(
    UUID requestId,
    Long roomId,
    Long bookingId,
    LocalDate startDate,
    LocalDate endDate,
    boolean active) {}
