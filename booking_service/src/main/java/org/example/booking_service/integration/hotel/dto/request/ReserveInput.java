package org.example.booking_service.integration.hotel.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record ReserveInput(
    UUID requestId,
    Long bookingId,
    Long roomId,
    Long hotelId,
    boolean autoSelect,
    LocalDate startDate,
    LocalDate endDate,
    UUID replaces) {}
