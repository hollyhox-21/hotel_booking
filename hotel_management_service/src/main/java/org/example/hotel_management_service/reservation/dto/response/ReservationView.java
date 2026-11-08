package org.example.hotel_management_service.reservation.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ReservationView(
    UUID requestId,
    Long roomId,
    Long bookingId,
    LocalDate startDate,
    LocalDate endDate,
    boolean active) {}
